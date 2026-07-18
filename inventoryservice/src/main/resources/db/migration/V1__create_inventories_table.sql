CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE inventory_item (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                sku VARCHAR(50) NOT NULL UNIQUE,
                                available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
                                reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inventory_reservation (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       order_id UUID NOT NULL,
                                       sku VARCHAR(50) NOT NULL,
                                       quantity INTEGER NOT NULL CHECK (quantity > 0),
                                       status VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
                                           CHECK (status IN ('RESERVED', 'RELEASED', 'CONFIRMED')),
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_reservation_order_id
    ON inventory_reservation(order_id);
