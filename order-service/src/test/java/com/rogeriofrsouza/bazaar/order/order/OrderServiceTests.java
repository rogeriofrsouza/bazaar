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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import(ContainersConfig.class)
class OrderServiceTests {

    @Autowired
    OrderService orderService;

    @MockitoBean
    CatalogClient catalogClient;

    @Test
    void placesOrderWithCatalogPrices() {
        UUID keyboard = UUID.randomUUID();
        UUID mouse = UUID.randomUUID();
        given(catalogClient.getProducts(anyCollection())).willReturn(List.of(
                new CatalogProduct(keyboard, "Keyboard", new BigDecimal("150.00")),
                new CatalogProduct(mouse, "Mouse", new BigDecimal("49.90"))
        ));

        OrderResponse placed = orderService.place(new CreateOrderRequest(List.of(
                new OrderItemRequest(keyboard, 2),
                new OrderItemRequest(mouse, 1)
        )));

        assertThat(placed.id().version()).isEqualTo(7);

        OrderResponse order = orderService.findById(placed.id());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.total()).isEqualByComparingTo("349.90");
        assertThat(order.items())
                .extracting(OrderItemResponse::productId, OrderItemResponse::productName,
                        OrderItemResponse::unitPrice, OrderItemResponse::quantity)
                .containsExactlyInAnyOrder(
                        tuple(keyboard, "Keyboard", new BigDecimal("150.00"), 2),
                        tuple(mouse, "Mouse", new BigDecimal("49.90"), 1)
                );
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

        assertThatThrownBy(() -> orderService.place(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(unknown.toString());
                });
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        assertThatThrownBy(() -> orderService.findById(UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
