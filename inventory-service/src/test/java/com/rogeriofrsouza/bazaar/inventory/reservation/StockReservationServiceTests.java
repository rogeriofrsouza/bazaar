package com.rogeriofrsouza.bazaar.inventory.reservation;

import com.rogeriofrsouza.bazaar.inventory.ContainersConfig;
import com.rogeriofrsouza.bazaar.inventory.item.CreateInventoryItemRequest;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemResponse;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemService;
import com.rogeriofrsouza.bazaar.inventory.outbox.Outbox;
import com.rogeriofrsouza.bazaar.inventory.reservation.OrderPlacedMessage.OrderPlacedItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Import(ContainersConfig.class)
@Transactional
class StockReservationServiceTests {

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private Outbox outbox;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void reservesAllItems() {
        UUID keyboard = createItem(10);
        UUID mouse = createItem(5);
        UUID orderId = UUID.randomUUID();

        stockReservationService.reserve(new OrderPlacedMessage(orderId, List.of(
                new OrderPlacedItem(keyboard, 2),
                new OrderPlacedItem(mouse, 1),
                new OrderPlacedItem(keyboard, 3)
        )));

        StockReservation reservation = stockReservationRepository.findByOrderId(orderId).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(reservation.quantities()).isEqualTo(Map.of(keyboard, 5, mouse, 1));
        assertThat(reservation.getId().version()).isEqualTo(7);
        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 5, 5));
        assertThat(inventoryItemService.findByProductId(mouse))
                .isEqualTo(new InventoryItemResponse(mouse, 5, 1, 4));
        assertThat(outboxMessages(orderId)).containsExactly(new StockReservedMessage(orderId));
    }

    @Test
    void rejectsWhenOneItemIsShort() {
        UUID keyboard = createItem(10);
        UUID mouse = createItem(1);
        UUID orderId = UUID.randomUUID();

        stockReservationService.reserve(new OrderPlacedMessage(orderId, List.of(
                new OrderPlacedItem(keyboard, 2),
                new OrderPlacedItem(mouse, 2)
        )));

        assertThat(stockReservationRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(StockReservationStatus.REJECTED);
        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 0, 10));
        assertThat(inventoryItemService.findByProductId(mouse))
                .isEqualTo(new InventoryItemResponse(mouse, 1, 0, 1));
        assertThat(outboxMessages(orderId)).singleElement()
                .isInstanceOfSatisfying(StockRejectedMessage.class,
                        message -> assertThat(message.reason()).contains(mouse.toString()));
    }

    @Test
    void rejectsWhenOneItemIsMissing() {
        UUID keyboard = createItem(10);
        UUID unknown = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        stockReservationService.reserve(new OrderPlacedMessage(orderId, List.of(
                new OrderPlacedItem(keyboard, 2),
                new OrderPlacedItem(unknown, 1)
        )));

        assertThat(stockReservationRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(StockReservationStatus.REJECTED);
        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 0, 10));
        assertThat(outboxMessages(orderId)).containsExactly(
                new StockRejectedMessage(orderId, "Inventory items not found: " + unknown));
    }

    @Test
    void ignoresDuplicateOrderPlaced() {
        UUID keyboard = createItem(10);
        UUID orderId = UUID.randomUUID();
        OrderPlacedMessage message = new OrderPlacedMessage(orderId, List.of(new OrderPlacedItem(keyboard, 2)));

        stockReservationService.reserve(message);
        stockReservationService.reserve(message);

        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 2, 8));
        assertThat(outboxMessages(orderId)).containsExactly(new StockReservedMessage(orderId));
    }

    @Test
    void fulfilsReservedStock() {
        UUID keyboard = createItem(10);
        UUID orderId = reserve(keyboard, 3);

        stockReservationService.fulfil(new OrderConfirmedMessage(orderId));
        stockReservationService.fulfil(new OrderConfirmedMessage(orderId));

        assertThat(stockReservationRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(StockReservationStatus.FULFILLED);
        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 7, 0, 7));
    }

    @Test
    void releasesReservedStock() {
        UUID keyboard = createItem(10);
        UUID orderId = reserve(keyboard, 3);

        stockReservationService.release(new OrderCancelledMessage(orderId));
        stockReservationService.release(new OrderCancelledMessage(orderId));

        assertThat(stockReservationRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(StockReservationStatus.RELEASED);
        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 0, 10));
    }

    @Test
    void ignoresFulfilAndReleaseWithoutReservedStock() {
        UUID keyboard = createItem(10);
        UUID orderId = reserve(keyboard, 3);
        stockReservationService.release(new OrderCancelledMessage(orderId));

        stockReservationService.fulfil(new OrderConfirmedMessage(orderId));
        stockReservationService.fulfil(new OrderConfirmedMessage(UUID.randomUUID()));
        stockReservationService.release(new OrderCancelledMessage(UUID.randomUUID()));

        assertThat(stockReservationRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(StockReservationStatus.RELEASED);
        assertThat(inventoryItemService.findByProductId(keyboard))
                .isEqualTo(new InventoryItemResponse(keyboard, 10, 0, 10));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void outboxNeedsTransaction() {
        assertThatExceptionOfType(IllegalTransactionStateException.class)
                .isThrownBy(() -> outbox.add(StockReservedMessage.ROUTING_KEY,
                        new StockReservedMessage(UUID.randomUUID())));
    }

    private UUID createItem(int quantityOnHand) {
        return inventoryItemService.create(new CreateInventoryItemRequest(UUID.randomUUID(), quantityOnHand))
                .productId();
    }

    private UUID reserve(UUID productId, int quantity) {
        UUID orderId = UUID.randomUUID();
        stockReservationService.reserve(
                new OrderPlacedMessage(orderId, List.of(new OrderPlacedItem(productId, quantity))));
        return orderId;
    }

    private List<Object> outboxMessages(UUID orderId) {
        return jdbcTemplate.query(
                "SELECT exchange, routing_key, payload FROM outbox_message WHERE payload LIKE ? ORDER BY created_at",
                (rs, rowNum) -> readMessage(rs.getString("exchange"), rs.getString("routing_key"),
                        rs.getString("payload")),
                "%" + orderId + "%");
    }

    private Object readMessage(String exchange, String routingKey, String payload) {
        assertThat(exchange).isEqualTo(Outbox.EXCHANGE);
        Class<?> type = switch (routingKey) {
            case StockReservedMessage.ROUTING_KEY -> StockReservedMessage.class;
            case StockRejectedMessage.ROUTING_KEY -> StockRejectedMessage.class;
            default -> throw new IllegalStateException("Unexpected routing key " + routingKey);
        };
        return jsonMapper.readValue(payload, type);
    }
}
