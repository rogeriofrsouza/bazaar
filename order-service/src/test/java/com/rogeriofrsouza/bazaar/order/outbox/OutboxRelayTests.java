package com.rogeriofrsouza.bazaar.order.outbox;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import({ContainersConfig.class, OutboxRelayTests.TestTopicConfig.class})
class OutboxRelayTests {

    private static final String TOPIC = "outbox-relay-tests";

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private Outbox outbox;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearOutbox() {
        jdbcTemplate.update("DELETE FROM outbox_message");
    }

    @Test
    void publishesPendingMessagesInOrderAndMarksThemSent() {
        long offset = endOffset(TOPIC);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        add(TOPIC, Map.of("orderId", first));
        add(TOPIC, Map.of("orderId", second));

        outboxRelay.relay();

        List<ConsumerRecord<String, String>> records = receiveFrom(TOPIC, offset, 2);
        assertThat(records.get(0).value()).contains(first.toString());
        assertThat(records.get(1).value()).contains(second.toString());
        assertThat(records).allSatisfy(record -> assertThat(record.topic()).isEqualTo(TOPIC));
        assertThat(outboxMessageRepository.findAll())
                .extracting(OutboxMessage::getTopic, OutboxRelayTests::isSent)
                .containsExactlyInAnyOrder(tuple(TOPIC, true), tuple(TOPIC, true));
    }

    @Test
    void keepsUnroutableMessagesPendingAndSendsTheRest() {
        long offset = endOffset(TOPIC);
        UUID routable = UUID.randomUUID();
        add("nobody.listens", Map.of("orderId", UUID.randomUUID()));
        add(TOPIC, Map.of("orderId", routable));

        outboxRelay.relay();

        List<ConsumerRecord<String, String>> records = receiveFrom(TOPIC, offset, 1);
        assertThat(records.getFirst().value()).contains(routable.toString());
        assertThat(outboxMessageRepository.findAll())
                .extracting(OutboxMessage::getTopic, OutboxRelayTests::isSent)
                .containsExactlyInAnyOrder(tuple("nobody.listens", false), tuple(TOPIC, true));
    }

    private long endOffset(String topic) {
        TopicPartition partition = new TopicPartition(topic, 0);
        try (Consumer<String, String> consumer = consumer()) {
            consumer.assign(List.of(partition));
            consumer.seekToEnd(List.of(partition));
            return consumer.position(partition);
        }
    }

    private List<ConsumerRecord<String, String>> receiveFrom(String topic, long offset, int count) {
        TopicPartition partition = new TopicPartition(topic, 0);
        try (Consumer<String, String> consumer = consumer()) {
            consumer.assign(List.of(partition));
            consumer.seek(partition, offset);

            List<ConsumerRecord<String, String>> records = new ArrayList<>();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (records.size() < count && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(200)).forEach(records::add);
            }
            assertThat(records).hasSize(count);
            return records;
        }
    }

    private Consumer<String, String> consumer() {
        return consumerFactory.createConsumer("outbox-relay-" + UUID.randomUUID(), "test");
    }

    private static boolean isSent(OutboxMessage outboxMessage) {
        return outboxMessage.getSentAt() != null;
    }

    private void add(String topic, Object message) {
        transactionTemplate.executeWithoutResult(status -> outbox.add(topic, message));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestTopicConfig {

        @Bean
        NewTopic outboxRelayTestTopic() {
            return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
        }
    }
}
