package com.rogeriofrsouza.bazaar.catalog.outbox;

import com.rogeriofrsouza.bazaar.catalog.ContainersConfig;
import com.rogeriofrsouza.bazaar.catalog.product.CatalogExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import({ContainersConfig.class, OutboxRelayTests.TestQueueConfig.class})
class OutboxRelayTests {

    private static final String QUEUE = "catalog-outbox-relay-tests";

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private Outbox outbox;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearOutbox() {
        jdbcTemplate.update("DELETE FROM outbox_message");
        amqpAdmin.purgeQueue(QUEUE);
    }

    @Test
    void publishesPendingMessagesAndMarksThemSent() {
        UUID productId = UUID.randomUUID();
        add(CatalogExchange.PRODUCT_CREATED, Map.of("productId", productId));

        outboxRelay.relay();

        Message message = rabbitTemplate.receive(QUEUE, 5_000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains(productId.toString());
        assertThat(message.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo(CatalogExchange.PRODUCT_CREATED);
        assertThat(outboxMessageRepository.findAll())
                .extracting(OutboxMessage::getRoutingKey, OutboxRelayTests::isSent)
                .containsExactly(tuple(CatalogExchange.PRODUCT_CREATED, true));
    }

    @Test
    void keepsUnroutableMessagesPending() {
        add("nobody.listens", Map.of("productId", UUID.randomUUID()));

        outboxRelay.relay();

        assertThat(rabbitTemplate.receive(QUEUE, 100)).isNull();
        assertThat(outboxMessageRepository.findAll())
                .extracting(OutboxMessage::getRoutingKey, OutboxRelayTests::isSent)
                .containsExactly(tuple("nobody.listens", false));
    }

    @Test
    void purgesSentMessagesOlderThanRetention() {
        Instant now = Instant.now();
        UUID expiredSentId = UUID.randomUUID();
        UUID recentSentId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();
        Instant oldCreatedAt = now.minus(Duration.ofDays(8));
        insertOutboxMessage(expiredSentId, oldCreatedAt, oldCreatedAt);
        insertOutboxMessage(recentSentId, oldCreatedAt, now.minus(Duration.ofHours(1)));
        insertOutboxMessage(pendingId, oldCreatedAt, null);

        assertThat(outbox.purgeSent()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT id FROM outbox_message", UUID.class))
                .containsExactlyInAnyOrder(recentSentId, pendingId);
    }

    private static boolean isSent(OutboxMessage outboxMessage) {
        return outboxMessage.getSentAt() != null;
    }

    private void insertOutboxMessage(UUID id, Instant createdAt, Instant sentAt) {
        jdbcTemplate.update("""
                INSERT INTO outbox_message (id, exchange, routing_key, payload, created_at, sent_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, Outbox.EXCHANGE, "test.message", "{}", Timestamp.from(createdAt),
                sentAt == null ? null : Timestamp.from(sentAt));
    }

    private void add(String routingKey, Object message) {
        transactionTemplate.executeWithoutResult(status -> outbox.add(routingKey, message));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestQueueConfig {

        @Bean
        Declarables outboxRelayTestQueue() {
            Queue queue = QueueBuilder.nonDurable(QUEUE).build();
            return new Declarables(
                    queue,
                    BindingBuilder.bind(queue).to(new TopicExchange(Outbox.EXCHANGE)).with("product.#")
            );
        }
    }
}
