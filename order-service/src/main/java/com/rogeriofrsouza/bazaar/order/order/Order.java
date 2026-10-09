package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table("orders")
public class Order {

    @Id
    private final UUID id;

    private final OrderStatus status;

    private final Currency currency;

    private final BigDecimal total;

    @MappedCollection(idColumn = "order_id")
    private final Set<OrderItem> items;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Order(
            UUID id,
            OrderStatus status,
            Currency currency,
            BigDecimal total,
            Set<OrderItem> items,
            Instant createdAt,
            Instant updatedAt
    ) {
        Assert.notEmpty(items, "An order needs at least one item");
        Assert.isTrue(total.compareTo(sumOfSubtotals(items)) == 0, "Order total must match its items");
        this.id = id;
        this.status = status;
        this.currency = currency;
        this.total = total;
        this.items = Set.copyOf(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order place(Currency currency, List<OrderItem> items) {
        Set<OrderItem> itemSet = Set.copyOf(items);
        return new Order(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                OrderStatus.PENDING,
                currency,
                sumOfSubtotals(itemSet),
                itemSet,
                null,
                null
        );
    }

    private static BigDecimal sumOfSubtotals(Set<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Set<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
