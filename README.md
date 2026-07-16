# Order Management System — PoC

Proof of Concept for an event-driven Order Management System built with Spring Boot, Apache Camel, Kafka, and PostgreSQL, following a microservices + Saga architecture.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [API Response Format](#api-response-format)
- [Order Service](#order-service)
  - [DB Schema](#order-service-db-schema)
  - [API Endpoints](#order-service-api-endpoints)
- [Inventory Service](#inventory-service)
  - [DB Schema](#inventory-service-db-schema)
  - [API Endpoints](#inventory-service-api-endpoints)
- [Payment Service](#payment-service)
  - [DB Schema](#payment-service-db-schema)
  - [API Endpoints](#payment-service-api-endpoints)
- [Shipment Service](#shipment-service)
  - [DB Schema](#shipment-service-db-schema)
  - [API Endpoints](#shipment-service-api-endpoints)
- [Notification Service](#notification-service)
  - [DB Schema](#notification-service-db-schema)
  - [API Endpoints](#notification-service-api-endpoints)
- [Saga Flow](#saga-flow-order-creation)
- [Kafka Topics](#kafka-topics)

---

## Architecture Overview

Each microservice owns its own PostgreSQL schema/database (database-per-service) and communicates with other services asynchronously via Kafka events, orchestrated/choreographed as a **Saga**. Apache Camel is used for routing, event transformation, and integration between Kafka and each service's internal processing pipeline.

```
                 ┌───────────────┐
   REST ───────► │ Order Service │
                 └───────┬───────┘
                         │ order.created
                         ▼
                 ┌───────────────┐        inventory.reserved /
                 │Inventory Svc  │ ─────► inventory.rejected
                 └───────┬───────┘
                         │ (on success)
                         ▼
                 ┌───────────────┐        payment.authorized /
                 │Payment Svc    │ ─────► payment.declined
                 └───────┬───────┘
                         │ (on success)
                         ▼
                 ┌───────────────┐        shipment.created /
                 │Shipment Svc   │ ─────► shipment.failed
                 └───────┬───────┘
                         │
                         ▼
                 ┌───────────────┐
                 │Notification   │  (listens to all above topics)
                 │Service        │
                 └───────────────┘
```

Services in this document:

| Service | Datastore | Responsibilities |
|---|---|---|
| **Order Service** | PostgreSQL (`orderservice` schema) | Order creation, order lifecycle status |
| **Inventory Service** | PostgreSQL (`inventoryservice` schema) | Stock reservation, release, replenishment |
| **Payment Service** | PostgreSQL (`paymentservice` schema) | Payment authorization, capture, refund (saga compensation) |
| **Shipment Service** | PostgreSQL (`shipmentservice` schema) | Shipment creation & tracking |
| **Notification Service** | PostgreSQL (`notificationservice` schema) | Customer notifications (email/SMS/push) |

> The schemas below for Inventory, Payment, Shipment, and Notification are designs meant to be flexible starting points — adjust as needed.
>
> **Why Payment Service:** in almost every real-world OMS saga, payment authorization sits between stock reservation and shipment — and its refund/void is a key compensating action if a later step (e.g. shipment) fails. It's the most commonly missing piece in a minimal Order/Inventory/Shipment/Notification slice, so it's included here. Other commonly-added pieces (Customer/Account Service, Product Catalog Service, API Gateway, a dedicated Saga Orchestrator/audit-log service) were intentionally left out of this POC's scope — flag if you'd like any of those added too.

---

## API Response Format

Every REST endpoint across every service in this system returns a common response envelope, so callers (and the Notification/Saga layers) can handle success and error cases uniformly regardless of which service they're talking to.

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Order fetched successfully",
  "response": { }
}
```

| Field | Type | Description |
|---|---|---|
| `statusCode` | int | HTTP status code |
| `statusMessage` | string | HTTP status reason phrase |
| `message` | string | Human-readable description of the result |
| `response` | object | The endpoint's actual payload on success, or `false` on error |

On error, `response` is `false` and `message` carries the error detail:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "message": "Order not found for id=b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
  "response": false
}
```

Order Service uses `ApiResponseDto` (`orderservice/src/main/java/com/oms/orderservice/dto/ApiResponseDto.java`) and `GlobalExceptionHandler` (`orderservice/src/main/java/com/oms/orderservice/exception/GlobalExceptionHandler.java`) for this. Every other service's endpoint examples below are shown pre-wrapped in this same envelope — each should reuse (or mirror) that same `ApiResponseDto` pattern rather than introducing a service-specific shape.

---

## Order Service

Spring Boot module: `orderservice/`

### Order Service DB Schema

Migration: `orderservice/src/main/resources/db/migration/V1__create_orders_table.sql`

#### Table: `order`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `customer_id` | UUID | NOT NULL, indexed (`idx_order_customer_id`) |
| `status` | VARCHAR(20) | NOT NULL, default `'PENDING'`, indexed (`idx_order_status`) |
| `total_amount` | NUMERIC(12,2) | NOT NULL |
| `cancellation_reason` | VARCHAR(255) | nullable — populated when `status` moves to `CANCEL_REQUESTED` via `PATCH /api/v1/orders/{id}/cancel` |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |
| `updated_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

Possible `status` values (`OrderStatus` enum, `orderservice/src/main/java/com/oms/orderservice/entity/OrderStatus.java`): `CREATED`, `PENDING`, `CONFIRMED`, `CANCEL_REQUESTED`, `CANCELLED`

**Order status lifecycle:** statuses form a strict, one-way chain — an order can only advance to the *next* status in the sequence below, never skip ahead, never go back, and never re-set its current status. This is enforced by `PATCH /api/v1/orders/{id}/status` (see below).

```
CREATED ──► PENDING ──► CONFIRMED ──► CANCEL_REQUESTED ──► CANCELLED
```

> Note: the `status` column default in the migration (`V1__create_orders_table.sql`) is `'PENDING'`, but the application always sets `CREATED` explicitly when persisting a new order, so every order's actual starting status is `CREATED`.

#### Table: `order_item`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `order_id` | UUID | NOT NULL, FK → `order(id)` `ON DELETE CASCADE`, indexed (`idx_order_item_order_id`) |
| `sku` | VARCHAR(50) | NOT NULL |
| `quantity` | INTEGER | NOT NULL, `CHECK (quantity > 0)` |
| `unit_price` | NUMERIC(12,2) | NOT NULL |

**Relationship:** `order` 1 ── * `order_item` (one order has many order items, cascade delete)

### Order Service API Endpoints

Base path: `/api/v1`

#### `POST /api/v1/orders`

Creates a new order with its line items. Total amount is server-computed as the sum of `quantity * unitPrice` across all items. Order is created with status `CREATED`.

**Request Body**

```json
{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "items": [
    {
      "sku": "SKU-1001",
      "quantity": 2,
      "unitPrice": 19.99
    },
    {
      "sku": "SKU-2002",
      "quantity": 1,
      "unitPrice": 49.50
    }
  ]
}
```

| Field | Type | Validation |
|---|---|---|
| `customerId` | UUID | required (`@NotNull`) |
| `items` | array | required, non-empty (`@NotEmpty`) |
| `items[].sku` | string | required, non-blank (`@NotBlank`) |
| `items[].quantity` | integer | required (`@NotNull`) |
| `items[].unitPrice` | decimal | required (`@NotNull`) |

**Response — `201 Created`**

```json
{
  "statusCode": 201,
  "statusMessage": "Created",
  "message": "Order created successfully",
  "response": {
    "id": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "CREATED",
    "totalAmount": 89.48
  }
}
```

| Field | Type | Description |
|---|---|---|
| `response.id` | UUID | Generated order ID |
| `response.status` | string | Order status at creation time (`CREATED`) |
| `response.totalAmount` | decimal | Server-computed sum of `quantity * unitPrice` for all items |

**Response — `400 Bad Request`** (request body fails `@Valid` constraints, e.g. missing `customerId`, empty `items`)

```json
{
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "message": "customerId: must not be null, items: must not be empty",
  "response": false
}
```

#### `GET /api/v1/orders/{id}`

Fetch a single order with its line items.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Order fetched successfully",
  "response": {
    "id": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "status": "CONFIRMED",
    "totalAmount": 89.48,
    "items": [
      { "sku": "SKU-1001", "quantity": 2, "unitPrice": 19.99 },
      { "sku": "SKU-2002", "quantity": 1, "unitPrice": 49.50 }
    ]
  }
}
```

**Response — `404 Not Found`**

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "message": "Order not found for id=b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
  "response": false
}
```

**Response — `400 Bad Request`** (path `id` is not a valid UUID)

```json
{
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "message": "id: invalid value 'not-a-uuid'",
  "response": false
}
```

### Additional Order Service API Endpoints

The following endpoints round out the service — for listing/searching orders, driving the saga's status transitions, and cancellation.

#### `GET /api/v1/orders?customerId={customerId}&status={status}&page={page}&size={size}`

List/search orders, filterable by customer and status, paginated.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Orders fetched successfully",
  "response": {
    "content": [
      {
        "id": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
        "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "status": "CONFIRMED",
        "totalAmount": 89.48
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `PATCH /api/v1/orders/{id}/status`

Updates order status — invoked internally by the saga as it consumes downstream events (`inventory.rejected`, `payment.declined`, `shipment.created`, `shipment.failed`), or by an admin/ops action.

**Request Body**

```json
{
  "status": "CONFIRMED"
}
```

| Field | Type | Validation |
|---|---|---|
| `status` | string | required, must be one of `CREATED`, `PENDING`, `CONFIRMED`, `CANCEL_REQUESTED`, `CANCELLED`, and must be exactly one step ahead of the order's current status in the lifecycle chain above |

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Order status updated successfully",
  "response": {
    "id": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "CONFIRMED"
  }
}
```

**Response — `409 Conflict`** (invalid state transition, e.g. `CONFIRMED` → `PENDING`)

```json
{
  "statusCode": 409,
  "statusMessage": "Conflict",
  "message": "Cannot transition order from CONFIRMED to PENDING",
  "response": false
}
```

#### `POST /api/v1/orders/{id}/cancel`

Cancels an order prior to shipment and kicks off saga compensation (stock release, payment refund) via published events.

**Request Body**

```json
{
  "reason": "CUSTOMER_REQUESTED"
}
```

| Field | Type | Description |
|---|---|---|
| `reason` | string | optional — persisted to `order.cancellation_reason` (see [Order Service DB Schema](#order-service-db-schema)) |

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Order cancelled successfully",
  "response": {
    "id": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "CANCELLED"
  }
}
```

**Response — `409 Conflict`** (order already shipped/delivered — cannot cancel)

```json
{
  "statusCode": 409,
  "statusMessage": "Conflict",
  "message": "Order b3e1a1a0-1234-4a2b-9c3d-abcdef123456 has already shipped",
  "response": false
}
```

---

## Inventory Service

Spring Boot module: `inventoryservice/`

### Inventory Service DB Schema

#### Table: `inventory_item`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `sku` | VARCHAR(50) | NOT NULL, UNIQUE |
| `available_quantity` | INTEGER | NOT NULL, `CHECK (available_quantity >= 0)` |
| `reserved_quantity` | INTEGER | NOT NULL, default `0`, `CHECK (reserved_quantity >= 0)` |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |
| `updated_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

#### Table: `inventory_reservation`

Tracks per-order stock reservations for saga compensation (release on rollback).

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `order_id` | UUID | NOT NULL, indexed |
| `sku` | VARCHAR(50) | NOT NULL |
| `quantity` | INTEGER | NOT NULL, `CHECK (quantity > 0)` |
| `status` | VARCHAR(20) | NOT NULL, default `'RESERVED'` — one of `RESERVED`, `RELEASED`, `CONFIRMED` |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |
| `updated_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

### Inventory Service API Endpoints

#### `POST /api/v1/inventory/reserve`

Reserves stock for an order's items (invoked directly, or consumed internally off the `order.created` Kafka event in a choreographed saga).

**Request Body**

```json
{
  "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
  "items": [
    { "sku": "SKU-1001", "quantity": 2 },
    { "sku": "SKU-2002", "quantity": 1 }
  ]
}
```

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Stock reserved successfully",
  "response": {
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "RESERVED",
    "reservations": [
      { "sku": "SKU-1001", "quantity": 2, "status": "RESERVED" },
      { "sku": "SKU-2002", "quantity": 1, "status": "RESERVED" }
    ]
  }
}
```

**Response — `409 Conflict`** (insufficient stock — triggers saga compensation)

```json
{
  "statusCode": 409,
  "statusMessage": "Conflict",
  "message": "Insufficient stock for SKU-2002",
  "response": false
}
```

#### `POST /api/v1/inventory/release`

Compensating action — releases a previously reserved quantity back to available stock (invoked on saga rollback, e.g. shipment failure).

**Request Body**

```json
{
  "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456"
}
```

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Stock released successfully",
  "response": {
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "RELEASED"
  }
}
```

#### `GET /api/v1/inventory/{sku}`

Fetch current stock levels for a SKU.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Inventory fetched successfully",
  "response": {
    "sku": "SKU-1001",
    "availableQuantity": 48,
    "reservedQuantity": 2
  }
}
```

---

## Payment Service

Spring Boot module: `paymentservice/`

### Payment Service DB Schema

#### Table: `payment`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `order_id` | UUID | NOT NULL, UNIQUE, indexed |
| `customer_id` | UUID | NOT NULL, indexed |
| `amount` | NUMERIC(12,2) | NOT NULL |
| `currency` | VARCHAR(3) | NOT NULL, default `'USD'` |
| `status` | VARCHAR(20) | NOT NULL, default `'PENDING'` — one of `PENDING`, `AUTHORIZED`, `CAPTURED`, `DECLINED`, `REFUNDED` |
| `payment_method` | VARCHAR(20) | NOT NULL — one of `CARD`, `WALLET`, `BANK_TRANSFER`, `COD` |
| `provider_reference` | VARCHAR(100) | nullable — external PSP transaction/charge ID |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |
| `updated_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

#### Table: `payment_transaction`

Audit trail of state transitions/attempts against a payment (authorize, capture, refund, decline), useful for saga replay and debugging.

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `payment_id` | UUID | NOT NULL, FK → `payment(id)` `ON DELETE CASCADE`, indexed |
| `type` | VARCHAR(20) | NOT NULL — one of `AUTHORIZE`, `CAPTURE`, `REFUND`, `DECLINE` |
| `status` | VARCHAR(20) | NOT NULL — one of `SUCCESS`, `FAILED` |
| `reason` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

### Payment Service API Endpoints

#### `POST /api/v1/payments/authorize`

Authorizes payment for an order's total amount (consumed internally off the `inventory.reserved` Kafka event in the saga, or invoked directly).

**Request Body**

```json
{
  "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 89.48,
  "currency": "USD",
  "paymentMethod": "CARD"
}
```

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Payment authorized successfully",
  "response": {
    "id": "7a1c2d3e-4444-4b2b-9111-abcdef111222",
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "AUTHORIZED",
    "providerReference": "ch_1N3T5x2eZvKYlo2C"
  }
}
```

**Response — `402 Payment Required`** (declined — triggers saga compensation, e.g. `inventory.release`)

```json
{
  "statusCode": 402,
  "statusMessage": "Payment Required",
  "message": "Payment declined: INSUFFICIENT_FUNDS",
  "response": false
}
```

#### `POST /api/v1/payments/{orderId}/capture`

Captures a previously authorized payment (typically invoked once shipment is confirmed).

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Payment captured successfully",
  "response": {
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "CAPTURED"
  }
}
```

#### `POST /api/v1/payments/{orderId}/refund`

Compensating action — refunds/voids a payment (invoked on saga rollback, e.g. shipment failure after capture).

**Request Body**

```json
{
  "reason": "SHIPMENT_FAILED"
}
```

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Payment refunded successfully",
  "response": {
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "REFUNDED"
  }
}
```

#### `GET /api/v1/payments/{orderId}`

Fetch payment status/details for an order.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Payment fetched successfully",
  "response": {
    "id": "7a1c2d3e-4444-4b2b-9111-abcdef111222",
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "amount": 89.48,
    "currency": "USD",
    "status": "CAPTURED",
    "paymentMethod": "CARD"
  }
}
```

---

## Shipment Service

Spring Boot module: `shipmentservice/`

### Shipment Service DB Schema

#### Table: `shipment`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `order_id` | UUID | NOT NULL, UNIQUE, indexed |
| `status` | VARCHAR(20) | NOT NULL, default `'PENDING'` — one of `PENDING`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `FAILED` |
| `carrier` | VARCHAR(50) | nullable |
| `tracking_number` | VARCHAR(100) | nullable |
| `shipping_address` | JSONB | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |
| `updated_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

#### Table: `shipment_event`

Audit trail of shipment status transitions.

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `shipment_id` | UUID | NOT NULL, FK → `shipment(id)` `ON DELETE CASCADE`, indexed |
| `status` | VARCHAR(20) | NOT NULL |
| `notes` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |

### Shipment Service API Endpoints

#### `POST /api/v1/shipments`

Creates a shipment for a confirmed order (consumed internally off the `inventory.reserved` Kafka event, or invoked directly).

**Request Body**

```json
{
  "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
  "shippingAddress": {
    "line1": "221B Baker Street",
    "city": "London",
    "postalCode": "NW16XE",
    "country": "UK"
  }
}
```

**Response — `201 Created`**

```json
{
  "statusCode": 201,
  "statusMessage": "Created",
  "message": "Shipment created successfully",
  "response": {
    "id": "9d6f0c1a-aaaa-4b2b-9111-abcdef654321",
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "PENDING"
  }
}
```

#### `PATCH /api/v1/shipments/{id}/status`

Updates shipment status (e.g. dispatch, deliver, mark failed).

**Request Body**

```json
{
  "status": "DISPATCHED",
  "carrier": "DHL",
  "trackingNumber": "1Z999AA10123456784"
}
```

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Shipment status updated successfully",
  "response": {
    "id": "9d6f0c1a-aaaa-4b2b-9111-abcdef654321",
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "DISPATCHED",
    "carrier": "DHL",
    "trackingNumber": "1Z999AA10123456784"
  }
}
```

#### `GET /api/v1/shipments/{orderId}`

Fetch shipment details/tracking status by order ID.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Shipment fetched successfully",
  "response": {
    "id": "9d6f0c1a-aaaa-4b2b-9111-abcdef654321",
    "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
    "status": "IN_TRANSIT",
    "carrier": "DHL",
    "trackingNumber": "1Z999AA10123456784"
  }
}
```

---

## Notification Service

Spring Boot module: `notificationservice/`

### Notification Service DB Schema

#### Table: `notification`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `order_id` | UUID | NOT NULL, indexed |
| `customer_id` | UUID | NOT NULL, indexed |
| `channel` | VARCHAR(20) | NOT NULL — one of `EMAIL`, `SMS`, `PUSH` |
| `event_type` | VARCHAR(50) | NOT NULL — e.g. `ORDER_CREATED`, `ORDER_CONFIRMED`, `SHIPMENT_DISPATCHED`, `ORDER_CANCELLED` |
| `status` | VARCHAR(20) | NOT NULL, default `'PENDING'` — one of `PENDING`, `SENT`, `FAILED` |
| `payload` | JSONB | NOT NULL — rendered message content/template variables |
| `created_at` | TIMESTAMPTZ | NOT NULL, default `now()` |
| `sent_at` | TIMESTAMPTZ | nullable |

### Notification Service API Endpoints

This service is primarily event-driven (consumes Kafka events from Order/Inventory/Shipment services via Camel routes). A small REST surface supports querying and manual re-send.

#### `GET /api/v1/notifications/{orderId}`

Fetch notification history for an order.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Notifications fetched successfully",
  "response": [
    {
      "id": "e2f1a123-0000-4a2b-9c3d-abcdef000001",
      "orderId": "b3e1a1a0-1234-4a2b-9c3d-abcdef123456",
      "channel": "EMAIL",
      "eventType": "ORDER_CREATED",
      "status": "SENT",
      "sentAt": "2026-07-16T10:15:30Z"
    }
  ]
}
```

#### `POST /api/v1/notifications/{id}/resend`

Manually re-triggers a failed notification.

**Response — `200 OK`**

```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "message": "Notification resent successfully",
  "response": {
    "id": "e2f1a123-0000-4a2b-9c3d-abcdef000001",
    "status": "SENT",
    "sentAt": "2026-07-16T10:20:00Z"
  }
}
```

---

## Saga Flow (Order Creation)

Choreography-based saga using Kafka events, routed/transformed by Apache Camel in each service:

1. **Order Service**: `POST /api/v1/orders` → persists order (`CREATED`) → publishes `order.created`
2. **Inventory Service**: consumes `order.created` → reserves stock → publishes `inventory.reserved` or `inventory.rejected`
3. **Payment Service**: consumes `inventory.reserved` → authorizes payment → publishes `payment.authorized` or `payment.declined`
4. **Shipment Service**: consumes `payment.authorized` → creates shipment → publishes `shipment.created` or `shipment.failed`; on success, Payment Service also captures the authorized payment
5. **Order Service**: consumes `shipment.created`/`inventory.rejected`/`payment.declined`/`shipment.failed` → updates order status to `CONFIRMED` or `CANCELLED`, triggering compensation on failure (e.g. `inventory.release`, and `payment.refund` if payment had already been captured)
6. **Notification Service**: consumes all of the above events → sends customer notifications at each stage

## Kafka Topics

| Topic | Producer | Consumers | Payload summary |
|---|---|---|---|
| `order.created` | Order Service | Inventory Service, Notification Service | `orderId`, `customerId`, `items[]` |
| `inventory.reserved` | Inventory Service | Payment Service, Order Service, Notification Service | `orderId`, `reservations[]` |
| `inventory.rejected` | Inventory Service | Order Service, Notification Service | `orderId`, `reason`, `unavailableItems[]` |
| `payment.authorized` | Payment Service | Shipment Service, Order Service, Notification Service | `orderId`, `paymentId`, `status` |
| `payment.declined` | Payment Service | Order Service, Inventory Service (compensation), Notification Service | `orderId`, `reason` |
| `shipment.created` | Shipment Service | Order Service, Payment Service (triggers capture), Notification Service | `orderId`, `shipmentId`, `status` |
| `shipment.failed` | Shipment Service | Order Service, Inventory Service (compensation), Payment Service (refund), Notification Service | `orderId`, `shipmentId`, `reason` |
| `order.status.updated` | Order Service | Notification Service | `orderId`, `status` |
