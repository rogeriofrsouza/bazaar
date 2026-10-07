CREATE TABLE orders
(
    id         uuid PRIMARY KEY,
    status     varchar(20)    NOT NULL,
    currency   varchar(3)     NOT NULL,
    total      numeric(12, 2) NOT NULL,
    version    bigint         NOT NULL,
    created_at timestamptz    NOT NULL DEFAULT NOW(),
    updated_at timestamptz    NOT NULL DEFAULT NOW()
);

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
