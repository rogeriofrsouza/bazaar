ALTER TABLE orders
    ADD COLUMN expires_at timestamptz;

UPDATE orders
SET expires_at = updated_at + INTERVAL '1 hour'
WHERE status = 'RESERVED';

CREATE INDEX idx_orders_reserved_expires_at ON orders (expires_at) WHERE status = 'RESERVED';
