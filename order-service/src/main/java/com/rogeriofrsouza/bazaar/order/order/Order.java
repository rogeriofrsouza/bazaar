package com.rogeriofrsouza.bazaar.order.order;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    private Order(String number, Currency currency) {
        this.number = number;
        this.currency = currency;
        this.status = OrderStatus.PENDING;
        this.total = BigDecimal.ZERO;
    }

    public static Order place(String number, Currency currency, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one item");
        }

        Order order = new Order(number, currency);
        items.forEach(order::addItem);
        return order;
    }

    public void addItem(OrderItem item) {
        item.assignTo(this);
        items.add(item);
        total = total.add(item.getSubtotal());
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
