package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private static final int BATCH_SIZE = 100;
    private static final long CONFIRM_TIMEOUT_SECONDS = 10;

    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;

    OutboxRelay(OutboxMessageRepository outboxMessageRepository, RabbitTemplate rabbitTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${bazaar.outbox.relay-delay:1s}")
    @Transactional
    void relay() {
        List<OutboxMessage> pending = outboxMessageRepository.findPendingForUpdate(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        Map<OutboxMessage, CorrelationData> published =
                rabbitTemplate.invoke(operations -> publish(operations, pending));
        awaitConfirms(published.values());

        Instant sentAt = Instant.now();
        List<OutboxMessage> confirmed = published.entrySet()
                .stream()
                .filter(entry -> isConfirmed(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        confirmed.forEach(outboxMessage -> outboxMessage.markSent(sentAt));
        outboxMessageRepository.saveAll(confirmed);
    }

    private static Map<OutboxMessage, CorrelationData> publish(RabbitOperations operations,
                                                               List<OutboxMessage> pending) {
        Map<OutboxMessage, CorrelationData> published = new LinkedHashMap<>();

        for (OutboxMessage outboxMessage : pending) {
            String id = outboxMessage.getId().toString();
            Message message = MessageBuilder.withBody(outboxMessage.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(id)
                    .build();
            CorrelationData correlationData = new CorrelationData(id);

            operations.send(outboxMessage.getExchange(), outboxMessage.getRoutingKey(), message, correlationData);
            published.put(outboxMessage, correlationData);
        }

        return published;
    }

    private static void awaitConfirms(Collection<CorrelationData> correlations) {
        CompletableFuture<?>[] confirms = correlations.stream()
                .map(CorrelationData::getFuture)
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(confirms).get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException _) {
            log.warn("Not every outbox message was confirmed within {} s", CONFIRM_TIMEOUT_SECONDS);
        }
    }

    private static boolean isConfirmed(CorrelationData correlationData) {
        Confirm confirm = correlationData.getFuture().getNow(null);
        if (!confirm.ack()) {
            log.warn("Outbox message {} was nacked and stays pending: {}", correlationData.getId(), confirm.reason());
            return false;
        }

        ReturnedMessage returned = correlationData.getReturned();
        if (returned != null) {
            log.warn("Outbox message {} was returned and stays pending: {}",
                    correlationData.getId(), returned.getReplyText());
            return false;
        }
        return true;
    }
}
