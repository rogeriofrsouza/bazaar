package com.rogeriofrsouza.bazaar.catalog.config;

import com.rogeriofrsouza.bazaar.catalog.outbox.Outbox;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class RabbitConfig {

    @Bean
    JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    TopicExchange catalogExchange() {
        return new TopicExchange(Outbox.EXCHANGE);
    }
}
