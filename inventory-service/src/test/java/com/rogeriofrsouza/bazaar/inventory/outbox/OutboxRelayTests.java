package com.rogeriofrsouza.bazaar.inventory.outbox;

import com.rogeriofrsouza.bazaar.inventory.ContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Import({ContainersConfig.class, OutboxRelayTests.TestQueueConfig.class})
class OutboxRelayTests {

    private static final String QUEUE = "outbox-relay-tests";

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
        UUID orderId = UUID.randomUUID();
        add("stock.reserved", Map.of("orderId", orderId));

        outboxRelay.relay();

        Message message = rabbitTemplate.receive(QUEUE, 5_000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains(orderId.toString());
        assertThat(message.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("stock.reserved");
        assertThat(outboxMessageRepository.findAll()).singleElement()
                .satisfies(outboxMessage -> assertThat(outboxMessage.getSentAt()).isNotNull());
    }

    @Test
    void keepsUnroutableMessagesPending() {
        add("nobody.listens", Map.of("orderId", UUID.randomUUID()));

        assertThatExceptionOfType(AmqpException.class).isThrownBy(outboxRelay::relay);

        assertThat(outboxMessageRepository.findAll()).singleElement()
                .satisfies(outboxMessage -> assertThat(outboxMessage.getSentAt()).isNull());
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
                    BindingBuilder.bind(queue).to(new TopicExchange(Outbox.EXCHANGE)).with("stock.#")
            );
        }
    }
}
