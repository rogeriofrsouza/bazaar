package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.ContainersConfig;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.order.OrderResponse.OrderItemResponse;
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
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct("AAAA2222", "Keyboard", new BigDecimal("150.00"), BRL),
                new CatalogProduct("BBBB3333", "Mouse", new BigDecimal("49.90"), BRL)
        ));

        OrderResponse placed = orderService.place(new CreateOrderRequest(List.of(
                new OrderItemRequest("AAAA2222", 2),
                new OrderItemRequest("BBBB3333", 1)
        )));

        OrderResponse order = orderService.findByNumber(placed.number());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.currency()).isEqualTo("BRL");
        assertThat(order.total()).isEqualByComparingTo("349.90");
        assertThat(order.items())
                .extracting(OrderItemResponse::productName, OrderItemResponse::unitPrice, OrderItemResponse::quantity)
                .containsExactlyInAnyOrder(
                        tuple("Keyboard", new BigDecimal("150.00"), 2),
                        tuple("Mouse", new BigDecimal("49.90"), 1)
                );
    }

    @Test
    void rejectsMixedCurrencies() {
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct("CCCC4444", "Monitor", new BigDecimal("900.00"), BRL),
                new CatalogProduct("DDDD5555", "Cable", new BigDecimal("9.99"), USD)
        ));

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest("CCCC4444", 1),
                new OrderItemRequest("DDDD5555", 1)
        ));

        assertThatThrownBy(() -> orderService.place(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsUnknownProducts() {
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct("EEEE6666", "Headset", new BigDecimal("199.00"), BRL)
        ));

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest("EEEE6666", 1),
                new OrderItemRequest("FFFF7777", 1)
        ));

        assertThatThrownBy(() -> orderService.place(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains("FFFF7777");
                });
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        assertThatThrownBy(() -> orderService.findByNumber("ZZZZ9999"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
