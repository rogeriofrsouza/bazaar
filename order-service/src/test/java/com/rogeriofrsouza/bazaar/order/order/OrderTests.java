package com.rogeriofrsouza.bazaar.order.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class OrderTests {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    void placeGeneratesUuidV7IdAndComputesTotal() {
        OrderItem keyboard = OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 2);
        OrderItem mouse = OrderItem.create(UUID.randomUUID(), "Mouse", new BigDecimal("49.90"), 1);

        Order order = Order.place(BRL, List.of(keyboard, mouse));

        assertThat(order.getId().version()).isEqualTo(7);
        assertThat(Instant.ofEpochMilli(order.getId().getMostSignificantBits() >>> 16))
                .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCurrency()).isEqualTo(BRL);
        assertThat(order.getTotal()).isEqualByComparingTo("349.90");
        assertThat(order.getItems()).containsExactlyInAnyOrder(keyboard, mouse);
        assertThat(order.getCreatedAt()).isNull();
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatIllegalArgumentException().isThrownBy(() -> Order.place(BRL, List.of()))
                .withMessage("An order needs at least one item");
    }

    @Test
    void rejectsTotalThatDoesNotMatchItems() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 2);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Order(UUID.randomUUID(), OrderStatus.PENDING, BRL, new BigDecimal("150.00"),
                        Set.of(item), null, null))
                .withMessage("Order total must match its items");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 0))
                .withMessage("Quantity must be positive");
    }
}
