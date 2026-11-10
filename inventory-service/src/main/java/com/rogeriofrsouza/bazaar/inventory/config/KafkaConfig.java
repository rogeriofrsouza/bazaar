package com.rogeriofrsouza.bazaar.inventory.config;

import com.rogeriofrsouza.bazaar.inventory.reservation.InventoryTopics;
import com.rogeriofrsouza.bazaar.inventory.reservation.OrdersTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class KafkaConfig {

    static final String DEAD_LETTER_SUFFIX = ".dlq";

    private static final int PARTITIONS = 1;
    private static final int REPLICATION_FACTOR = 1;
    private static final long RETRIES = 2;

    @Bean
    StringJacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new StringJacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<String, String> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (failedRecord, _) -> new TopicPartition(failedRecord.topic() + DEAD_LETTER_SUFFIX, failedRecord.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, RETRIES));
    }

    @Bean
    KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(
                topic(InventoryTopics.RESERVED),
                topic(InventoryTopics.REJECTED),
                topic(OrdersTopics.PLACED),
                topic(OrdersTopics.CONFIRMED),
                topic(OrdersTopics.CANCELLED),
                topic(OrdersTopics.PLACED + DEAD_LETTER_SUFFIX),
                topic(OrdersTopics.CONFIRMED + DEAD_LETTER_SUFFIX),
                topic(OrdersTopics.CANCELLED + DEAD_LETTER_SUFFIX)
        );
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICATION_FACTOR).build();
    }
}
