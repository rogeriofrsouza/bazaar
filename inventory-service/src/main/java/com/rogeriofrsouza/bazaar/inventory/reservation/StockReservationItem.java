package com.rogeriofrsouza.bazaar.inventory.reservation;

import org.springframework.util.Assert;

import java.util.UUID;

public class StockReservationItem {

    private final UUID productId;

    private final int quantity;

    public StockReservationItem(UUID productId, int quantity) {
        Assert.isTrue(quantity > 0, "Quantity must be positive");
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
