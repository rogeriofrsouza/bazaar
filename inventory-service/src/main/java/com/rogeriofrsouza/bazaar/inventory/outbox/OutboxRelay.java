package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
class OutboxRelay {

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

        for (OutboxMessage outboxMessage : pending) {
            send(outboxMessage);
            outboxMessage.markSent(Instant.now());
        }

        outboxMessageRepository.saveAll(pending);
    }

    private void send(OutboxMessage outboxMessage) {
        String id = outboxMessage.getId().toString();
        Message message = MessageBuilder.withBody(outboxMessage.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(id)
                .build();
        CorrelationData correlationData = new CorrelationData(id);

        rabbitTemplate.send(outboxMessage.getExchange(), outboxMessage.getRoutingKey(), message, correlationData);

        Confirm confirm = awaitConfirm(correlationData);
        if (!confirm.ack()) {
            throw new AmqpException("Outbox message " + id + " was nacked: " + confirm.reason());
        }
        ReturnedMessage returned = correlationData.getReturned();
        if (returned != null) {
            throw new AmqpException("Outbox message " + id + " was returned: " + returned.getReplyText());
        }
    }

    private static Confirm awaitConfirm(CorrelationData correlationData) {
        try {
            return correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while waiting for a publisher confirm", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new AmqpException("No publisher confirm for outbox message " + correlationData.getId(), ex);
        }
    }
}
