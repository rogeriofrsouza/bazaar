package com.rogeriofrsouza.bazaar.order.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private static final int BATCH_SIZE = 100;
    private static final long CONFIRM_TIMEOUT_SECONDS = 10;

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    OutboxRelay(OutboxMessageRepository outboxMessageRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${bazaar.outbox.relay-delay:1s}")
    @Transactional
    void relay() {
        List<OutboxMessage> pending = outboxMessageRepository.findPendingForUpdate(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        Map<OutboxMessage, CompletableFuture<SendResult<String, String>>> published = publish(pending);
        awaitSends(published.values());

        Instant sentAt = Instant.now();
        List<OutboxMessage> acknowledged = published.entrySet()
                .stream()
                .filter(entry -> isAcknowledged(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        acknowledged.forEach(outboxMessage -> outboxMessage.markSent(sentAt));
        outboxMessageRepository.saveAll(acknowledged);
    }

    private Map<OutboxMessage, CompletableFuture<SendResult<String, String>>> publish(List<OutboxMessage> pending) {
        Map<OutboxMessage, CompletableFuture<SendResult<String, String>>> published = new LinkedHashMap<>();

        for (OutboxMessage outboxMessage : pending) {
            published.put(outboxMessage, send(outboxMessage));
        }

        return published;
    }

    private CompletableFuture<SendResult<String, String>> send(OutboxMessage outboxMessage) {
        try {
            return kafkaTemplate.send(
                    outboxMessage.getTopic(), outboxMessage.getId().toString(), outboxMessage.getPayload());
        } catch (RuntimeException ex) {
            // One message failing to send must not abort the batch: the rest still gets published,
            // and the acknowledged rows are marked sent. This row stays pending.
            return CompletableFuture.failedFuture(ex);
        }
    }

    private static void awaitSends(Collection<CompletableFuture<SendResult<String, String>>> sends) {
        try {
            CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new))
                    .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException _) {
            log.warn("Not every outbox message was acknowledged within {} s", CONFIRM_TIMEOUT_SECONDS);
        }
    }

    private static boolean isAcknowledged(OutboxMessage outboxMessage,
                                          CompletableFuture<SendResult<String, String>> send) {
        if (!send.isDone() || send.isCancelled()) {
            log.warn("Outbox message {} was not acknowledged and stays pending", outboxMessage.getId());
            return false;
        }

        try {
            send.join();
            return true;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            log.warn("Outbox message {} was not acknowledged and stays pending", outboxMessage.getId(), cause);
            return false;
        }
    }
}
