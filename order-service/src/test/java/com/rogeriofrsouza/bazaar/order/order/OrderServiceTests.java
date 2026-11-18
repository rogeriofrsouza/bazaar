package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.order.OrderPlacedMessage.OrderPlacedItem;
import com.rogeriofrsouza.bazaar.order.order.OrderResponse.OrderItemResponse;
import com.rogeriofrsouza.bazaar.order.outbox.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Import(ContainersConfig.class)
class OrderServiceTests {

    @Autowired
    OrderService orderService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    OrderExpiryJob orderExpiryJob;

    @MockitoBean
    CatalogClient catalogClient;

    @MockitoSpyBean
    Outbox outbox;

    @Test
    void placesPendingOrderWithCatalogPrices() {
        UUID keyboard = UUID.randomUUID();
        UUID mouse = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(keyboard, "Keyboard", new BigDecimal("150.00")),
                new CatalogProduct(mouse, "Mouse", new BigDecimal("49.90"))
        ));

        OrderResponse placed = orderService.place("subject-1", new CreateOrderRequest(List.of(
                new OrderItemRequest(keyboard, 2),
                new OrderItemRequest(mouse, 1)
        )));

        assertThat(placed.id().version()).isEqualTo(7);

        OrderResponse order = orderService.findById(placed.id(), "subject-1");

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.rejectionReason()).isNull();
        assertThat(order.total()).isEqualByComparingTo("349.90");
        assertThat(order.items())
                .extracting(OrderItemResponse::productId, OrderItemResponse::productName,
                        OrderItemResponse::unitPrice, OrderItemResponse::quantity)
                .containsExactlyInAnyOrder(
                        tuple(keyboard, "Keyboard", new BigDecimal("150.00"), 2),
                        tuple(mouse, "Mouse", new BigDecimal("49.90"), 1)
                );
        assertThat(outboxRoutingKeys(placed.id())).containsExactly(OrdersExchange.PLACED);

        OrderPlacedMessage message = jsonMapper.readValue(
                outboxPayload(placed.id(), OrdersExchange.PLACED), OrderPlacedMessage.class);
        assertThat(message.orderId()).isEqualTo(placed.id());
        assertThat(message.items()).containsExactlyInAnyOrder(
                new OrderPlacedItem(keyboard, 2),
                new OrderPlacedItem(mouse, 1)
        );
    }

    @Test
    void rejectsUnknownProducts() {
        UUID headset = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(headset, "Headset", new BigDecimal("199.00"))
        ));
        String owner = "subject-" + UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(headset, 1),
                new OrderItemRequest(unknown, 1)
        ));

        assertThatThrownBy(() -> orderService.place(owner, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(unknown.toString());
                });

        assertThat(orderService.findAll(owner, PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void callsCatalogOutsideTheTransaction() {
        UUID productId = UUID.randomUUID();
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        given(catalogClient.getProducts(anyCollection())).willAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of(new CatalogProduct(productId, "Keyboard", new BigDecimal("150.00")));
        });

        OrderResponse placed = orderService.place("subject-1", new CreateOrderRequest(List.of(
                new OrderItemRequest(productId, 1)
        )));

        assertThat(transactionActive).isFalse();
        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.PENDING);
        assertThat(outboxRoutingKeys(placed.id())).containsExactly(OrdersExchange.PLACED);
    }

    @Test
    void savesNothingWhenCatalogIsUnavailable() {
        UUID productId = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable"));
        String owner = "subject-" + UUID.randomUUID();
        Integer outboxRows = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_message", Integer.class);

        assertThatThrownBy(() -> orderService.place(owner, new CreateOrderRequest(List.of(
                new OrderItemRequest(productId, 1)
        ))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(orderService.findAll(owner, PageRequest.of(0, 20))).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_message", Integer.class))
                .isEqualTo(outboxRows);
    }

    @Test
    void marksOrderReservedOnce() {
        OrderResponse placed = placeOrder("subject-1");

        orderService.markReserved(new StockReservedMessage(placed.id()));
        orderService.markReserved(new StockReservedMessage(placed.id()));
        orderService.markRejected(new StockRejectedMessage(placed.id(), "Out of stock"));

        OrderResponse order = orderService.findById(placed.id(), "subject-1");
        assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(order.rejectionReason()).isNull();
        assertThat(order.expiresAt()).isCloseTo(Instant.now().plus(1, ChronoUnit.HOURS), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void marksOrderRejectedWithReason() {
        OrderResponse placed = placeOrder("subject-1");

        orderService.markRejected(new StockRejectedMessage(placed.id(), "Only 1 units available"));
        orderService.markReserved(new StockReservedMessage(placed.id()));

        OrderResponse order = orderService.findById(placed.id(), "subject-1");
        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.rejectionReason()).isEqualTo("Only 1 units available");
    }

    @Test
    void ignoresStockMessagesForUnknownOrders() {
        UUID unknown = UUID.randomUUID();

        orderService.markReserved(new StockReservedMessage(unknown));
        orderService.markRejected(new StockRejectedMessage(unknown, "Out of stock"));

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.findById(unknown, "subject-1"));
    }

    @Test
    void confirmsReservedOrder() {
        OrderResponse placed = reservedOrder("subject-1");

        OrderResponse confirmed = orderService.confirm(placed.id(), "subject-1");

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(outboxRoutingKeys(placed.id()))
                .containsExactly(OrdersExchange.PLACED, OrdersExchange.CONFIRMED);
    }

    @Test
    void cancelsReservedOrder() {
        OrderResponse placed = reservedOrder("subject-1");

        OrderResponse cancelled = orderService.cancel(placed.id(), "subject-1");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(outboxRoutingKeys(placed.id()))
                .containsExactly(OrdersExchange.PLACED, OrdersExchange.CANCELLED);
    }

    @Test
    void rejectsConfirmingPendingOrder() {
        OrderResponse placed = placeOrder("subject-1");

        assertThatExceptionOfType(OrderNotReservedException.class)
                .isThrownBy(() -> orderService.confirm(placed.id(), "subject-1"));

        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.PENDING);
        assertThat(outboxRoutingKeys(placed.id())).containsExactly(OrdersExchange.PLACED);
    }

    @Test
    void hidesOtherCustomersOrdersFromTransitions() {
        OrderResponse placed = reservedOrder("subject-1");

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.cancel(placed.id(), "subject-2"));

        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(outboxRoutingKeys(placed.id())).containsExactly(OrdersExchange.PLACED);
    }

    @Test
    @Transactional
    void expiresOverdueReservedOrdersInOneBatch() {
        OrderResponse first = reservedOrder("subject-1");
        OrderResponse second = reservedOrder("subject-1");
        setDeadline(first.id(), Instant.now().minusSeconds(2));
        setDeadline(second.id(), Instant.now().minusSeconds(1));

        orderService.expireOverdueOrders();
        orderService.expireOverdueOrders();

        for (OrderResponse order : List.of(first, second)) {
            assertThat(orderService.findById(order.id(), "subject-1").status()).isEqualTo(OrderStatus.EXPIRED);
            assertThat(outboxRoutingKeys(order.id()))
                    .containsExactly(OrdersExchange.PLACED, OrdersExchange.EXPIRED);
            OrderExpiredMessage message = jsonMapper.readValue(
                    outboxPayload(order.id(), OrdersExchange.EXPIRED), OrderExpiredMessage.class);
            assertThat(message).isEqualTo(new OrderExpiredMessage(order.id()));
        }
        assertThatExceptionOfType(OrderNotReservedException.class)
                .isThrownBy(() -> orderService.confirm(first.id(), "subject-1"))
                .withMessage("Order " + first.id() + " is EXPIRED, not RESERVED");
    }

    @Test
    @Transactional
    void doesNotExpireOrdersThatAreNotOverdue() {
        OrderResponse due = reservedOrder("subject-1");
        OrderResponse confirmed = reservedOrder("subject-1");
        orderService.confirm(confirmed.id(), "subject-1");
        setDeadline(confirmed.id(), Instant.now().minusSeconds(1));
        OrderResponse cancelled = reservedOrder("subject-1");
        orderService.cancel(cancelled.id(), "subject-1");
        setDeadline(cancelled.id(), Instant.now().minusSeconds(1));
        OrderResponse pending = placeOrder("subject-1");
        OrderResponse rejected = placeOrder("subject-1");
        orderService.markRejected(new StockRejectedMessage(rejected.id(), "Out of stock"));

        orderService.expireOverdueOrders();

        assertThat(orderService.findById(due.id(), "subject-1").status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(orderService.findById(confirmed.id(), "subject-1").status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.findById(cancelled.id(), "subject-1").status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderService.findById(pending.id(), "subject-1").status()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderService.findById(rejected.id(), "subject-1").status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(outboxRoutingKeys(due.id())).containsExactly(OrdersExchange.PLACED);
        assertThat(outboxRoutingKeys(confirmed.id()))
                .containsExactly(OrdersExchange.PLACED, OrdersExchange.CONFIRMED);
        assertThat(outboxRoutingKeys(cancelled.id()))
                .containsExactly(OrdersExchange.PLACED, OrdersExchange.CANCELLED);
        assertThat(outboxRoutingKeys(pending.id())).containsExactly(OrdersExchange.PLACED);
        assertThat(outboxRoutingKeys(rejected.id())).containsExactly(OrdersExchange.PLACED);
    }

    @Test
    void rollsBackEntireExpiryBatchWhenOutboxFails() {
        OrderResponse first = reservedOrder("subject-1");
        OrderResponse second = reservedOrder("subject-1");
        setDeadline(first.id(), Instant.now().minusSeconds(2));
        setDeadline(second.id(), Instant.now().minusSeconds(1));
        Outbox target = AopTestUtils.getUltimateTargetObject(outbox);
        doThrow(new IllegalStateException("Outbox unavailable"))
                .when(target).add(OrdersExchange.EXPIRED, new OrderExpiredMessage(second.id()));

        try {
            assertThatThrownBy(orderService::expireOverdueOrders)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Outbox unavailable");

            for (OrderResponse order : List.of(first, second)) {
                assertThat(orderService.findById(order.id(), "subject-1").status()).isEqualTo(OrderStatus.RESERVED);
                assertThat(outboxRoutingKeys(order.id())).containsExactly(OrdersExchange.PLACED);
            }
        } finally {
            setDeadline(first.id(), Instant.now().plus(1, ChronoUnit.HOURS));
            setDeadline(second.id(), Instant.now().plus(1, ChronoUnit.HOURS));
        }
    }

    @Test
    @Transactional
    void expiryJobExpiresOverdueOrders() {
        OrderResponse overdue = reservedOrder("subject-1");
        OrderResponse due = reservedOrder("subject-1");
        setDeadline(overdue.id(), Instant.now().minusSeconds(1));

        orderExpiryJob.expireOverdueOrders();

        assertThat(orderService.findById(overdue.id(), "subject-1").status()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(outboxRoutingKeys(overdue.id()))
                .containsExactly(OrdersExchange.PLACED, OrdersExchange.EXPIRED);
        assertThat(orderService.findById(due.id(), "subject-1").status()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void listsOnlyOwnOrdersNewestFirst() {
        String owner = "subject-" + UUID.randomUUID();

        OrderResponse first = placeOrder(owner);
        OrderResponse second = placeOrder(owner);
        placeOrder("subject-" + UUID.randomUUID());

        Page<OrderResponse> page = orderService.findAll(
                owner, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(OrderResponse::id).containsExactly(second.id(), first.id());
    }

    @Test
    void hidesOrdersOfOtherCustomers() {
        OrderResponse placed = placeOrder("subject-1");

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.findById(placed.id(), "subject-2"));
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.findById(id, "subject-1"))
                .withMessage("Order " + id + " not found");
    }

    private OrderResponse placeOrder(String customerSubject) {
        UUID productId = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(productId, "Keyboard", new BigDecimal("150.00"))
        ));
        return orderService.place(customerSubject, new CreateOrderRequest(List.of(
                new OrderItemRequest(productId, 1)
        )));
    }

    private OrderResponse reservedOrder(String customerSubject) {
        OrderResponse placed = placeOrder(customerSubject);
        orderService.markReserved(new StockReservedMessage(placed.id()));
        return placed;
    }

    private List<String> outboxRoutingKeys(UUID orderId) {
        return jdbcTemplate.queryForList(
                "SELECT routing_key FROM outbox_message WHERE exchange = ? AND payload LIKE ? ORDER BY created_at",
                String.class, Outbox.EXCHANGE, "%" + orderId + "%");
    }

    private void setDeadline(UUID orderId, Instant expiresAt) {
        jdbcTemplate.update("UPDATE orders SET expires_at = ? WHERE id = ?", Timestamp.from(expiresAt), orderId);
    }

    private String outboxPayload(UUID orderId, String routingKey) {
        return jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_message WHERE routing_key = ? AND payload LIKE ?",
                String.class, routingKey, "%" + orderId + "%");
    }
}
