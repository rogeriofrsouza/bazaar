CREATE TABLE stock_reservation
(
    id         uuid PRIMARY KEY,
    order_id   uuid        NOT NULL UNIQUE,
    status     varchar(20) NOT NULL,
    version    bigint      NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE stock_reservation_item
(
    reservation_id uuid    NOT NULL REFERENCES stock_reservation (id),
    product_id     uuid    NOT NULL,
    quantity       integer NOT NULL,
    PRIMARY KEY (reservation_id, product_id)
);
