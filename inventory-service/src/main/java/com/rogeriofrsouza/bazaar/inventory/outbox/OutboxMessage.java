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

    private final String exchange;

    private final String routingKey;

    private final String payload;

    private final Instant createdAt;

    private @Nullable Instant sentAt;

    public OutboxMessage(
            UUID id,
            String exchange,
            String routingKey,
            String payload,
            Instant createdAt,
            @Nullable Instant sentAt
    ) {
        Assert.hasText(exchange, "Exchange must not be blank");
        Assert.hasText(routingKey, "Routing key must not be blank");
        Assert.hasText(payload, "Payload must not be blank");
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static OutboxMessage create(String exchange, String routingKey, String payload) {
        return new OutboxMessage(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                exchange,
                routingKey,
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

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
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
