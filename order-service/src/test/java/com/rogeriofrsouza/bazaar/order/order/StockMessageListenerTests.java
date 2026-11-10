package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(ContainersConfig.class)
class StockMessageListenerTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void marksOrderReservedWhenStockIsReserved() {
        UUID orderId = placeOrder();

        send(InventoryTopics.RESERVED, new StockReservedMessage(orderId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId))
                        .hasValueSatisfying(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED)));
    }

    @Test
    void marksOrderRejectedWhenStockIsRejected() {
        UUID orderId = placeOrder();

        send(InventoryTopics.REJECTED, new StockRejectedMessage(orderId, "Only 1 units available"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId)).hasValueSatisfying(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
                    assertThat(order.getRejectionReason()).isEqualTo("Only 1 units available");
                }));
    }

    private void send(String topic, Object message) {
        kafkaTemplate.send(topic, jsonMapper.writeValueAsString(message)).join();
    }

    private UUID placeOrder() {
        return orderRepository.save(Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 1)
        ))).getId();
    }
}
