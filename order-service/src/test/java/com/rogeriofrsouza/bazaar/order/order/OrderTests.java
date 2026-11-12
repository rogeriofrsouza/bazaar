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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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
                        new BigDecimal("150.00"), Set.of(item), null, null, null, null))
                .withMessage("Order total must match its items");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 0))
                .withMessage("Quantity must be positive");
    }

    @Test
    void reservesPendingOrder() {
        Order order = pendingOrder();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        order.reserve(expiresAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(order.getRejectionReason()).isNull();
        assertThat(order.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void rejectsPendingOrderWithReason() {
        Order order = pendingOrder();

        order.reject("Only 1 units available");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("Only 1 units available");
    }

    @Test
    void rejectsBlankRejectionReason() {
        Order order = pendingOrder();

        assertThatIllegalArgumentException().isThrownBy(() -> order.reject(" "))
                .withMessage("Rejection reason must not be blank");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void reservesOrRejectsOnlyPendingOrders() {
        Order order = reservedOrder();

        assertThatExceptionOfType(OrderNotPendingException.class).isThrownBy(() -> order.reserve(Instant.now()))
                .withMessage("Order " + order.getId() + " is RESERVED, not PENDING");
        assertThatExceptionOfType(OrderNotPendingException.class).isThrownBy(() -> order.reject("Out of stock"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void confirmsReservedOrder() {
        Order order = reservedOrder();

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelsReservedOrder() {
        Order order = reservedOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmsOrCancelsOnlyReservedOrders() {
        Order order = pendingOrder();

        assertThatExceptionOfType(OrderNotReservedException.class).isThrownBy(order::confirm)
                .withMessage("Order " + order.getId() + " is PENDING, not RESERVED");
        assertThatExceptionOfType(OrderNotReservedException.class).isThrownBy(order::cancel);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        order.reserve(Instant.now());
        order.cancel();

        assertThatExceptionOfType(OrderNotReservedException.class).isThrownBy(order::confirm)
                .withMessage("Order " + order.getId() + " is CANCELLED, not RESERVED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void expiresOverdueReservedOrder() {
        Instant expiresAt = Instant.now();
        Order order = pendingOrder();
        order.reserve(expiresAt);

        assertThat(order.isOverdue(expiresAt.minusMillis(1))).isFalse();
        assertThat(order.isOverdue(expiresAt)).isTrue();

        order.expire(expiresAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.isOverdue(expiresAt)).isFalse();
    }

    @Test
    void doesNotExpireBeforeDeadline() {
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        Order order = pendingOrder();
        order.reserve(expiresAt);

        assertThatIllegalStateException().isThrownBy(() -> order.expire(Instant.now()))
                .withMessage("Order " + order.getId() + " does not expire until " + expiresAt);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void expiresOnlyReservedOrders() {
        Instant later = Instant.now().plus(2, ChronoUnit.HOURS);
        Order pending = pendingOrder();
        Order confirmed = reservedOrder();
        confirmed.confirm();

        assertThat(pending.isOverdue(later)).isFalse();
        assertThat(confirmed.isOverdue(later)).isFalse();
        assertThatExceptionOfType(OrderNotReservedException.class).isThrownBy(() -> pending.expire(later));
        assertThatExceptionOfType(OrderNotReservedException.class).isThrownBy(() -> confirmed.expire(later));
    }

    private static Order pendingOrder() {
        return Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 1)));
    }

    private static Order reservedOrder() {
        Order order = pendingOrder();
        order.reserve(Instant.now().plus(1, ChronoUnit.HOURS));
        return order;
    }
}
