CREATE TABLE product
(
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        varchar(8)     NOT NULL UNIQUE,
    name        varchar(200)   NOT NULL,
    description text,
    price       numeric(12, 2) NOT NULL CHECK (price >= 0),
    currency    varchar(3)     NOT NULL,
    image_url   varchar(255),
    status      varchar(20)    NOT NULL,
    category_id bigint         NOT NULL REFERENCES category (id),
    created_at  timestamptz    NOT NULL DEFAULT now(),
    updated_at  timestamptz    NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_category_id ON product (category_id);
CREATE INDEX idx_product_status ON product (status);
