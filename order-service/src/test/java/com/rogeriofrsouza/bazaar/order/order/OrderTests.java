package com.rogeriofrsouza.bazaar.order.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class OrderTests {

    @Test
    void placeGeneratesUuidV7IdAndComputesTotal() {
        OrderItem keyboard = OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 2);
        OrderItem mouse = OrderItem.create(UUID.randomUUID(), "Mouse", new BigDecimal("49.90"), 1);

        Order order = Order.place("subject-1", List.of(keyboard, mouse));

        assertThat(order.getId().version()).isEqualTo(7);
        assertThat(Instant.ofEpochMilli(order.getId().getMostSignificantBits() >>> 16))
                .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(order.getCustomerSubject()).isEqualTo("subject-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualByComparingTo("349.90");
        assertThat(order.getItems()).containsExactlyInAnyOrder(keyboard, mouse);
        assertThat(order.getCreatedAt()).isNull();
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatIllegalArgumentException().isThrownBy(() -> Order.place("subject-1", List.of()))
                .withMessage("An order needs at least one item");
    }

    @Test
    void rejectsBlankCustomerSubject() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 1);

        assertThatIllegalArgumentException().isThrownBy(() -> Order.place(" ", List.of(item)))
                .withMessage("Customer subject must not be blank");
    }

    @Test
    void rejectsTotalThatDoesNotMatchItems() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 2);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Order(UUID.randomUUID(), "subject-1", OrderStatus.PENDING,
                        new BigDecimal("150.00"), Set.of(item), null, null))
                .withMessage("Order total must match its items");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 0))
                .withMessage("Quantity must be positive");
    }

    @Test
    void confirmsPendingOrder() {
        Order order = pendingOrder();

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelsPendingOrder() {
        Order order = pendingOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void movesOnlyPendingOrders() {
        Order order = pendingOrder();
        order.cancel();

        assertThatExceptionOfType(OrderNotPendingException.class).isThrownBy(order::confirm)
                .withMessage("Order " + order.getId() + " is CANCELLED, not PENDING");
        assertThatExceptionOfType(OrderNotPendingException.class).isThrownBy(order::cancel);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private static Order pendingOrder() {
        return Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 1)));
    }
}
