CREATE TABLE customer
(
    id         uuid PRIMARY KEY,
    subject    varchar(255) NOT NULL UNIQUE,
    first_name varchar(100) NOT NULL,
    last_name  varchar(100) NOT NULL,
    email      varchar(255) NOT NULL,
    phone      varchar(30),
    version    bigint       NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT NOW(),
    updated_at timestamptz  NOT NULL DEFAULT NOW()
);
