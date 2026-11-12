package com.rogeriofrsouza.bazaar.order.order;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table("orders")
public class Order {

    @Id
    private final UUID id;

    private final String customerSubject;

    private OrderStatus status;

    private final BigDecimal total;

    @MappedCollection(idColumn = "order_id")
    private final Set<OrderItem> items;

    private @Nullable String rejectionReason;

    private @Nullable Instant expiresAt;

    @Version
    private @Nullable Long version;

    @CreatedDate
    private @Nullable Instant createdAt;

    @LastModifiedDate
    private @Nullable Instant updatedAt;

    public Order(
            UUID id,
            String customerSubject,
            OrderStatus status,
            BigDecimal total,
            Set<OrderItem> items,
            @Nullable String rejectionReason,
            @Nullable Instant expiresAt,
            @Nullable Instant createdAt,
            @Nullable Instant updatedAt
    ) {
        Assert.hasText(customerSubject, "Customer subject must not be blank");
        Assert.notEmpty(items, "An order needs at least one item");
        Assert.isTrue(total.compareTo(sumOfSubtotals(items)) == 0, "Order total must match its items");
        this.id = id;
        this.customerSubject = customerSubject;
        this.status = status;
        this.total = total;
        this.items = Set.copyOf(items);
        this.rejectionReason = rejectionReason;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order place(String customerSubject, List<OrderItem> items) {
        Set<OrderItem> itemSet = Set.copyOf(items);
        return new Order(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                customerSubject,
                OrderStatus.PENDING,
                sumOfSubtotals(itemSet),
                itemSet,
                null,
                null,
                null,
                null
        );
    }

    public void reserve(Instant expiresAt) {
        requirePending();
        status = OrderStatus.RESERVED;
        this.expiresAt = expiresAt;
    }

    public void reject(String reason) {
        Assert.hasText(reason, "Rejection reason must not be blank");
        requirePending();
        status = OrderStatus.REJECTED;
        rejectionReason = reason;
    }

    public void confirm() {
        requireReserved();
        status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        requireReserved();
        status = OrderStatus.CANCELLED;
    }

    public boolean isOverdue(Instant now) {
        return status == OrderStatus.RESERVED && expiresAt != null && !expiresAt.isAfter(now);
    }

    public void expire(Instant now) {
        requireReserved();
        Assert.state(isOverdue(now), () -> "Order " + id + " does not expire until " + expiresAt);
        status = OrderStatus.EXPIRED;
    }

    private void requirePending() {
        if (status != OrderStatus.PENDING) {
            throw new OrderNotPendingException(id, status);
        }
    }

    private void requireReserved() {
        if (status != OrderStatus.RESERVED) {
            throw new OrderNotReservedException(id, status);
        }
    }

    private static BigDecimal sumOfSubtotals(Set<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerSubject() {
        return customerSubject;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Set<OrderItem> getItems() {
        return items;
    }

    public @Nullable String getRejectionReason() {
        return rejectionReason;
    }

    public @Nullable Instant getExpiresAt() {
        return expiresAt;
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
