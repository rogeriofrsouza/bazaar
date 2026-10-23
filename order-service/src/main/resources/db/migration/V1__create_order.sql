CREATE TABLE orders
(
    id               uuid PRIMARY KEY,
    customer_subject varchar(255)   NOT NULL,
    status           varchar(20)    NOT NULL,
    total            numeric(12, 2) NOT NULL,
    version          bigint         NOT NULL,
    created_at       timestamptz    NOT NULL DEFAULT NOW(),
    updated_at       timestamptz    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_customer_subject ON orders (customer_subject);

CREATE TABLE order_item
(
    id           uuid PRIMARY KEY,
    order_id     uuid           NOT NULL REFERENCES orders (id),
    product_id   uuid           NOT NULL,
    product_name varchar(200)   NOT NULL,
    unit_price   numeric(12, 2) NOT NULL,
    quantity     integer        NOT NULL
);

CREATE INDEX idx_order_item_order_id ON order_item (order_id);
