package com.rogeriofrsouza.bazaar.inventory.reservation;

import com.rogeriofrsouza.bazaar.inventory.ContainersConfig;
import com.rogeriofrsouza.bazaar.inventory.item.CreateInventoryItemRequest;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemResponse;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemService;
import com.rogeriofrsouza.bazaar.inventory.reservation.OrderPlacedMessage.OrderPlacedItem;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(ContainersConfig.class)
class OrderMessageListenerTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reservesStockForPlacedOrder() {
        UUID keyboard = inventoryItemService.create(new CreateInventoryItemRequest(UUID.randomUUID(), 10)).productId();
        UUID orderId = UUID.randomUUID();

        rabbitTemplate.convertAndSend("bazaar.orders", OrderPlacedMessage.ROUTING_KEY,
                new OrderPlacedMessage(orderId, List.of(new OrderPlacedItem(keyboard, 2))));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(stockReservationRepository.findByOrderId(orderId))
                        .hasValueSatisfying(reservation ->
                                assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RESERVED)));

        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 2, 8));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_message WHERE routing_key = ? AND payload LIKE ? AND sent_at IS NULL",
                Integer.class, StockReservedMessage.ROUTING_KEY, "%" + orderId + "%"))
                .isEqualTo(1);
    }
}
