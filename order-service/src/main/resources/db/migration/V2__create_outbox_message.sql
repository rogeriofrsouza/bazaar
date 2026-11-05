CREATE TABLE outbox_message
(
    id          uuid PRIMARY KEY,
    exchange    varchar(255) NOT NULL,
    routing_key varchar(255) NOT NULL,
    payload     text         NOT NULL,
    created_at  timestamptz  NOT NULL,
    sent_at     timestamptz
);

CREATE INDEX idx_outbox_message_pending ON outbox_message (created_at) WHERE sent_at IS NULL;
