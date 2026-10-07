CREATE TABLE inventory_item
(
    id                uuid PRIMARY KEY,
    product_id        uuid        NOT NULL UNIQUE,
    quantity_on_hand  integer     NOT NULL,
    quantity_reserved integer     NOT NULL DEFAULT 0,
    version           bigint      NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);
