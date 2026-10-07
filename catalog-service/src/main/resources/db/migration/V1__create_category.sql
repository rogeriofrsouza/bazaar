CREATE TABLE category
(
    id        uuid PRIMARY KEY,
    slug      varchar(100) NOT NULL UNIQUE,
    name      varchar(100) NOT NULL,
    parent_id uuid REFERENCES category (id)
);
