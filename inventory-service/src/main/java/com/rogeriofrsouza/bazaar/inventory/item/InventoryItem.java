package com.rogeriofrsouza.bazaar.inventory.item;

import io.hypersistence.tsid.TSID;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int quantityReserved;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    private InventoryItem(Long productId, int quantityOnHand) {
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
    }

    public static InventoryItem create(Long productId, int quantityOnHand) {
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand must not be negative");
        }
        return new InventoryItem(productId, quantityOnHand);
    }

    public void restock(int quantity) {
        requirePositive(quantity);
        quantityOnHand += quantity;
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (quantity > getAvailable()) {
            throw new InsufficientStockException(
                    "Only " + getAvailable() + " units of " + TSID.from(productId) + " available");
        }
        quantityReserved += quantity;
    }

    public void release(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InsufficientStockException(
                    "Only " + quantityReserved + " units of " + TSID.from(productId) + " reserved");
        }
        quantityReserved -= quantity;
    }

    public void fulfil(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InsufficientStockException(
                    "Only " + quantityReserved + " units of " + TSID.from(productId) + " reserved");
        }
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
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
