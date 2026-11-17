CREATE INDEX idx_outbox_message_sent ON outbox_message (sent_at) WHERE sent_at IS NOT NULL;
