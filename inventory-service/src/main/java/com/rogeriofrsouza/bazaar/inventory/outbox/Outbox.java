package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

@Component
public class Outbox {

    public static final String EXCHANGE = "bazaar.inventory";

    private static final int BATCH_SIZE = 500;

    private final JdbcAggregateOperations jdbcAggregateOperations;
    private final OutboxMessageRepository outboxMessageRepository;
    private final JsonMapper jsonMapper;
    private final Duration retention;

    Outbox(JdbcAggregateOperations jdbcAggregateOperations, OutboxMessageRepository outboxMessageRepository,
           JsonMapper jsonMapper, @Value("${bazaar.outbox.retention:7d}") Duration retention) {
        this.jdbcAggregateOperations = jdbcAggregateOperations;
        this.outboxMessageRepository = outboxMessageRepository;
        this.jsonMapper = jsonMapper;
        this.retention = retention;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String routingKey, Object message) {
        String payload = jsonMapper.writeValueAsString(message);
        jdbcAggregateOperations.insert(OutboxMessage.create(EXCHANGE, routingKey, payload));
    }

    @Transactional
    public int purgeSent() {
        return outboxMessageRepository.deleteSentBefore(Instant.now().minus(retention), BATCH_SIZE);
    }
}
