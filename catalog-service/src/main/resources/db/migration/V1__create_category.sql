CREATE TABLE category
(
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug      varchar(100) NOT NULL UNIQUE,
    name      varchar(100) NOT NULL,
    parent_id bigint REFERENCES category (id)
);
