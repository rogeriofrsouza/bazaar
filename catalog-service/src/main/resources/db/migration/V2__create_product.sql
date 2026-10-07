CREATE TABLE product
(
    id          uuid PRIMARY KEY,
    name        varchar(200)   NOT NULL,
    description text,
    price       numeric(12, 2) NOT NULL CHECK (price >= 0),
    currency    varchar(3)     NOT NULL,
    image_url   varchar(255),
    status      varchar(20)    NOT NULL,
    category_id uuid           NOT NULL REFERENCES category (id),
    version     bigint         NOT NULL,
    created_at  timestamptz    NOT NULL DEFAULT NOW(),
    updated_at  timestamptz    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_category_id ON product (category_id);
CREATE INDEX idx_product_status ON product (status);
