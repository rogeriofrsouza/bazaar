CREATE TABLE category
(
    id        bigint PRIMARY KEY,
    slug      varchar(100) NOT NULL UNIQUE,
    name      varchar(100) NOT NULL,
    parent_id bigint REFERENCES category (id)
);
