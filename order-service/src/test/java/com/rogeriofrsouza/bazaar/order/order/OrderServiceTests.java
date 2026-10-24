package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.inventory.InventoryClient;
import com.rogeriofrsouza.bazaar.order.inventory.ReservationRequest;
import com.rogeriofrsouza.bazaar.order.inventory.ReservationRequest.ReservationItemRequest;
import com.rogeriofrsouza.bazaar.order.order.OrderResponse.OrderItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@SpringBootTest
@Import(ContainersConfig.class)
class OrderServiceTests {

    @Autowired
    OrderService orderService;

    @MockitoBean
    CatalogClient catalogClient;

    @MockitoBean
    InventoryClient inventoryClient;

    @Test
    void placesOrderWithCatalogPrices() {
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
        assertThat(order.total()).isEqualByComparingTo("349.90");
        assertThat(order.items())
                .extracting(OrderItemResponse::productId, OrderItemResponse::productName,
                        OrderItemResponse::unitPrice, OrderItemResponse::quantity)
                .containsExactlyInAnyOrder(
                        tuple(keyboard, "Keyboard", new BigDecimal("150.00"), 2),
                        tuple(mouse, "Mouse", new BigDecimal("49.90"), 1)
                );
        then(inventoryClient).should().reserve(new ReservationRequest(List.of(
                new ReservationItemRequest(keyboard, 2),
                new ReservationItemRequest(mouse, 1)
        )));
        then(inventoryClient).should(never()).release(any());
    }

    @Test
    void rejectsOrderWhenStockIsShort() {
        UUID monitor = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(monitor, "Monitor", new BigDecimal("900.00"))
        ));
        willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Only 1 units available"))
                .given(inventoryClient).reserve(any());
        String owner = "subject-" + UUID.randomUUID();

        assertThatThrownBy(() -> orderService.place(owner, new CreateOrderRequest(List.of(
                new OrderItemRequest(monitor, 2)
        ))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(orderService.findAll(owner, PageRequest.of(0, 20))).isEmpty();
        then(inventoryClient).should(never()).release(any());
    }

    @Test
    void releasesStockWhenOrderIsNotSaved() {
        UUID cable = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(cable, "C".repeat(201), new BigDecimal("9.99"))
        ));

        assertThatThrownBy(() -> orderService.place("subject-1", new CreateOrderRequest(List.of(
                new OrderItemRequest(cable, 3)
        ))))
                .isInstanceOf(DataAccessException.class);

        ReservationRequest reservation = new ReservationRequest(List.of(new ReservationItemRequest(cable, 3)));
        then(inventoryClient).should().reserve(reservation);
        then(inventoryClient).should().release(reservation);
    }

    @Test
    void rejectsUnknownProducts() {
        UUID headset = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(headset, "Headset", new BigDecimal("199.00"))
        ));

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(headset, 1),
                new OrderItemRequest(unknown, 1)
        ));

        assertThatThrownBy(() -> orderService.place("subject-1", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(unknown.toString());
                });
    }

    @Test
    void listsOnlyOwnOrdersNewestFirst() {
        UUID cable = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(cable, "Cable", new BigDecimal("9.99"))
        ));
        String owner = "subject-" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(cable, 1)));

        OrderResponse first = orderService.place(owner, request);
        OrderResponse second = orderService.place(owner, request);
        orderService.place("subject-" + UUID.randomUUID(), request);

        Page<OrderResponse> page = orderService.findAll(
                owner, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(OrderResponse::id).containsExactly(second.id(), first.id());
    }

    @Test
    void hidesOrdersOfOtherCustomers() {
        UUID cable = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(cable, "Cable", new BigDecimal("9.99"))
        ));

        OrderResponse placed = orderService.place("subject-1", new CreateOrderRequest(List.of(
                new OrderItemRequest(cable, 1)
        )));

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

    @Test
    void confirmsOrderAndFulfilsStock() {
        UUID keyboard = UUID.randomUUID();
        OrderResponse placed = placeOrder("subject-1", keyboard, 2);

        OrderResponse confirmed = orderService.confirm(placed.id(), "subject-1");

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.CONFIRMED);
        then(inventoryClient).should().fulfil(new ReservationRequest(List.of(new ReservationItemRequest(keyboard, 2))));
    }

    @Test
    void cancelsOrderAndReleasesStock() {
        UUID keyboard = UUID.randomUUID();
        OrderResponse placed = placeOrder("subject-1", keyboard, 2);

        OrderResponse cancelled = orderService.cancel(placed.id(), "subject-1");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.CANCELLED);
        then(inventoryClient).should()
                .release(new ReservationRequest(List.of(new ReservationItemRequest(keyboard, 2))));
    }

    @Test
    void rejectsConfirmingCancelledOrder() {
        OrderResponse placed = placeOrder("subject-1", UUID.randomUUID(), 1);
        orderService.cancel(placed.id(), "subject-1");

        assertThatExceptionOfType(OrderNotPendingException.class)
                .isThrownBy(() -> orderService.confirm(placed.id(), "subject-1"));

        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.CANCELLED);
        then(inventoryClient).should(never()).fulfil(any());
    }

    @Test
    void hidesOtherCustomersOrdersFromTransitions() {
        OrderResponse placed = placeOrder("subject-1", UUID.randomUUID(), 1);

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.cancel(placed.id(), "subject-2"));

        assertThat(orderService.findById(placed.id(), "subject-1").status()).isEqualTo(OrderStatus.PENDING);
        then(inventoryClient).should(never()).release(any());
    }

    private OrderResponse placeOrder(String customerSubject, UUID productId, int quantity) {
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(productId, "Keyboard", new BigDecimal("150.00"))
        ));
        return orderService.place(customerSubject, new CreateOrderRequest(List.of(
                new OrderItemRequest(productId, quantity)
        )));
    }
}
