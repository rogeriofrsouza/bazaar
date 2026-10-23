CREATE TABLE address
(
    id          uuid PRIMARY KEY,
    customer_id uuid         NOT NULL REFERENCES customer (id),
    line1       varchar(200) NOT NULL,
    line2       varchar(200),
    city        varchar(100) NOT NULL,
    state       varchar(100) NOT NULL,
    postal_code varchar(20)  NOT NULL,
    country     varchar(2)   NOT NULL
);

CREATE INDEX idx_address_customer_id ON address (customer_id);
