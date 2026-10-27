package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class Outbox {

    public static final String EXCHANGE = "bazaar.inventory";

    private final JdbcAggregateOperations jdbcAggregateOperations;
    private final JsonMapper jsonMapper;

    Outbox(JdbcAggregateOperations jdbcAggregateOperations, JsonMapper jsonMapper) {
        this.jdbcAggregateOperations = jdbcAggregateOperations;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String routingKey, Object message) {
        String payload = jsonMapper.writeValueAsString(message);
        jdbcAggregateOperations.insert(OutboxMessage.create(EXCHANGE, routingKey, payload));
    }
}
