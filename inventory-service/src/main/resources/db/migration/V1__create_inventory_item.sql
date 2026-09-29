CREATE TABLE inventory_item
(
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_code      varchar(8)  NOT NULL UNIQUE,
    quantity_on_hand  integer     NOT NULL,
    quantity_reserved integer     NOT NULL DEFAULT 0,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);
