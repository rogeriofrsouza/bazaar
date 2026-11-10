package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

@Table("outbox_message")
public class OutboxMessage {

    @Id
    private final UUID id;

    private final String topic;

    private final String payload;

    private final Instant createdAt;

    private @Nullable Instant sentAt;

    public OutboxMessage(
            UUID id,
            String topic,
            String payload,
            Instant createdAt,
            @Nullable Instant sentAt
    ) {
        Assert.hasText(topic, "Topic must not be blank");
        Assert.hasText(payload, "Payload must not be blank");
        this.id = id;
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static OutboxMessage create(String topic, String payload) {
        return new OutboxMessage(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                topic,
                payload,
                Instant.now(),
                null
        );
    }

    public void markSent(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getSentAt() {
        return sentAt;
    }
}
