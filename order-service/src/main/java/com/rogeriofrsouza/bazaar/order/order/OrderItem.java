package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    @Id
    private final UUID id;

    private final UUID productId;

    private final String productName;

    private final BigDecimal unitPrice;

    private final int quantity;

    public OrderItem(UUID id, UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        Assert.hasText(productName, "Product name must not be blank");
        Assert.isTrue(unitPrice.signum() >= 0, "Unit price must not be negative");
        Assert.isTrue(quantity > 0, "Quantity must be positive");
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItem create(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        return new OrderItem(
                UUID.ofEpochMillis(System.currentTimeMillis()), productId, productName, unitPrice, quantity);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
