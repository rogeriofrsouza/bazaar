package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.order.OrderResponse.OrderItemResponse;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import(ContainersConfig.class)
class OrderServiceTests {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    OrderService orderService;

    @MockitoBean
    CatalogClient catalogClient;

    @Test
    void placesOrderWithCatalogPrices() {
        TSID keyboard = TSID.Factory.getTsid();
        TSID mouse = TSID.Factory.getTsid();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(keyboard, "Keyboard", new BigDecimal("150.00"), BRL),
                new CatalogProduct(mouse, "Mouse", new BigDecimal("49.90"), BRL)
        ));

        OrderResponse placed = orderService.place(new CreateOrderRequest(List.of(
                new OrderItemRequest(keyboard, 2),
                new OrderItemRequest(mouse, 1)
        )));

        OrderResponse order = orderService.findById(TSID.from(placed.id()).toLong());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.currency()).isEqualTo("BRL");
        assertThat(order.total()).isEqualByComparingTo("349.90");
        assertThat(order.items())
                .extracting(OrderItemResponse::productId, OrderItemResponse::productName,
                        OrderItemResponse::unitPrice, OrderItemResponse::quantity)
                .containsExactlyInAnyOrder(
                        tuple(keyboard.toString(), "Keyboard", new BigDecimal("150.00"), 2),
                        tuple(mouse.toString(), "Mouse", new BigDecimal("49.90"), 1)
                );
    }

    @Test
    void rejectsMixedCurrencies() {
        TSID monitor = TSID.Factory.getTsid();
        TSID cable = TSID.Factory.getTsid();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(monitor, "Monitor", new BigDecimal("900.00"), BRL),
                new CatalogProduct(cable, "Cable", new BigDecimal("9.99"), USD)
        ));

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(monitor, 1),
                new OrderItemRequest(cable, 1)
        ));

        assertThatThrownBy(() -> orderService.place(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsUnknownProducts() {
        TSID headset = TSID.Factory.getTsid();
        TSID unknown = TSID.Factory.getTsid();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(headset, "Headset", new BigDecimal("199.00"), BRL)
        ));

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(headset, 1),
                new OrderItemRequest(unknown, 1)
        ));

        assertThatThrownBy(() -> orderService.place(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(unknown.toString());
                });
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        assertThatThrownBy(() -> orderService.findById(TSID.Factory.getTsid().toLong()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
