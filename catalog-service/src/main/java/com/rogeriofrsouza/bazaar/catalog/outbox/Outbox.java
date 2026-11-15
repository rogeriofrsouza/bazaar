package com.rogeriofrsouza.bazaar.catalog.outbox;

import com.rogeriofrsouza.bazaar.catalog.product.CatalogExchange;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class Outbox {

    public static final String EXCHANGE = CatalogExchange.NAME;

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
