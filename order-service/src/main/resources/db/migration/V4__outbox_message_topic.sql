ALTER TABLE outbox_message
    ADD COLUMN topic varchar(255) NOT NULL;

ALTER TABLE outbox_message
    DROP COLUMN exchange;

ALTER TABLE outbox_message
    DROP COLUMN routing_key;
