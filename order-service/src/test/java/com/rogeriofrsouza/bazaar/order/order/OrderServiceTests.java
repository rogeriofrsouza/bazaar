package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.order.OrderPlacedMessage.OrderPlacedItem;
import com.rogeriofrsouza.bazaar.order.order.OrderResponse.OrderItemResponse;
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
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import(ContainersConfig.class)
class OrderServiceTests {

    @Autowired
    OrderService orderService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    CatalogClient catalogClient;

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
        assertThat(outboxTopics(placed.id())).containsExactly(OrdersTopics.PLACED);

        OrderPlacedMessage message = jsonMapper.readValue(outboxPayload(placed.id()), OrderPlacedMessage.class);
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
    void marksOrderReservedOnce() {
        OrderResponse placed = placeOrder("subject-1");

        orderService.markReserved(new StockReservedMessage(placed.id()));
        orderService.markReserved(new StockReservedMessage(placed.id()));
        orderService.markRejected(new StockRejectedMessage(placed.id(), "Out of stock"));

        OrderResponse order = orderService.findById(placed.id(), "subject-1");
        assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(order.rejectionReason()).isNull();
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
        assertThat(outboxTopics(placed.id()))
                .containsExactly(OrdersTopics.PLACED, OrdersTopics.CONFIRMED);
    }

    @Test
    void cancelsReservedOrder() {
        OrderResponse placed = reservedOrder("subject-1");

        OrderResponse cancelled = orderService.cancel(placed.id(), "subject-1");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(outboxTopics(placed.id()))
                .containsExactly(OrdersTopics.PLACED, OrdersTopics.CANCELLED);
    }

    @Test
    void rejectsConfirmingPendingOrder() {
        OrderResponse placed = placeOrder("subject-1");

        assertThatExceptionOfType(OrderNotReservedException.class)
                .isThrownBy(() -> orderService.confirm(placed.id(), "subject-1"));

        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.PENDING);
        assertThat(outboxTopics(placed.id())).containsExactly(OrdersTopics.PLACED);
    }

    @Test
    void hidesOtherCustomersOrdersFromTransitions() {
        OrderResponse placed = reservedOrder("subject-1");

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.cancel(placed.id(), "subject-2"));

        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(outboxTopics(placed.id())).containsExactly(OrdersTopics.PLACED);
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

    private List<String> outboxTopics(UUID orderId) {
        return jdbcTemplate.queryForList(
                "SELECT topic FROM outbox_message WHERE payload LIKE ? ORDER BY created_at",
                String.class, "%" + orderId + "%");
    }

    private String outboxPayload(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_message WHERE topic = ? AND payload LIKE ?",
                String.class, OrdersTopics.PLACED, "%" + orderId + "%");
    }
}
