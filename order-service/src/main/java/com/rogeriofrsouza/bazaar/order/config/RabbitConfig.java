package com.rogeriofrsouza.bazaar.order.config;

import com.rogeriofrsouza.bazaar.order.order.InventoryExchange;
import com.rogeriofrsouza.bazaar.order.outbox.Outbox;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class RabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE = "bazaar.dlx";

    @Bean
    JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(Outbox.EXCHANGE);
    }

    @Bean
    TopicExchange inventoryExchange() {
        return new TopicExchange(InventoryExchange.NAME);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Declarables stockQueues(TopicExchange inventoryExchange, DirectExchange deadLetterExchange) {
        Map<String, String> routingKeys = Map.of(
                InventoryExchange.RESERVED_QUEUE, InventoryExchange.RESERVED,
                InventoryExchange.REJECTED_QUEUE, InventoryExchange.REJECTED
        );

        List<Declarable> declarables = new ArrayList<>();
        routingKeys.forEach((queueName, routingKey) -> {
            String deadLetterQueueName = queueName + ".dlq";
            Queue queue = QueueBuilder.durable(queueName)
                    .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(deadLetterQueueName)
                    .build();
            Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();

            declarables.add(queue);
            declarables.add(BindingBuilder.bind(queue).to(inventoryExchange).with(routingKey));
            declarables.add(deadLetterQueue);
            declarables.add(BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterQueueName));
        });
        return new Declarables(declarables);
    }
}
