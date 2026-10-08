package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

public class InventoryItem {

    @Id
    private final UUID id;

    private final UUID productId;

    private int quantityOnHand;

    private int quantityReserved;

    @Version
    private Long version;

    @CreatedDate
    @InsertOnlyProperty
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public InventoryItem(
            UUID id,
            UUID productId,
            int quantityOnHand,
            int quantityReserved,
            Instant createdAt,
            Instant updatedAt
    ) {
        Assert.isTrue(quantityOnHand >= 0, "Quantity on hand must not be negative");
        Assert.isTrue(quantityReserved >= 0, "Quantity reserved must not be negative");
        this.id = id;
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InventoryItem create(UUID productId, int quantityOnHand) {
        return new InventoryItem(
                UUID.ofEpochMillis(System.currentTimeMillis()), productId, quantityOnHand, 0, null, null);
    }

    public void restock(int quantity) {
        requirePositive(quantity);
        quantityOnHand += quantity;
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (quantity > getAvailable()) {
            throw new InsufficientStockException(
                    "Only " + getAvailable() + " units of " + productId + " available");
        }
        quantityReserved += quantity;
    }

    public void release(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InsufficientStockException(
                    "Only " + quantityReserved + " units of " + productId + " reserved");
        }
        quantityReserved -= quantity;
    }

    public void fulfil(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InsufficientStockException(
                    "Only " + quantityReserved + " units of " + productId + " reserved");
        }
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getAvailable() {
        return quantityOnHand - quantityReserved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
