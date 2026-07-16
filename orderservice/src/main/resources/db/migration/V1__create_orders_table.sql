-- Enable pgcrypto for gen_random_uuid() (safe if already enabled)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE "order" (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        customer_id UUID NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        total_amount NUMERIC(12,2) NOT NULL,
                        cancellation_reason VARCHAR(255),
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_item (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id UUID NOT NULL,
                             sku VARCHAR(50) NOT NULL,
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price NUMERIC(12,2) NOT NULL,

                             CONSTRAINT fk_order_item_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES "order"(id)
                                     ON DELETE CASCADE
);

CREATE INDEX idx_order_item_order_id
    ON order_item(order_id);

CREATE INDEX idx_order_customer_id
    ON "order"(customer_id);

CREATE INDEX idx_order_status
    ON "order"(status);