package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String productCode;

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

    private InventoryItem(String productCode, int quantityOnHand) {
        this.productCode = productCode;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
    }

    public static InventoryItem create(String productCode, int quantityOnHand) {
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand must not be negative");
        }
        return new InventoryItem(productCode, quantityOnHand);
    }

    public void restock(int quantity) {
        requirePositive(quantity);
        quantityOnHand += quantity;
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (quantity > getAvailable()) {
            throw new InsufficientStockException(
                    "Only " + getAvailable() + " units of " + productCode + " available");
        }
        quantityReserved += quantity;
    }

    public void release(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InsufficientStockException(
                    "Only " + quantityReserved + " units of " + productCode + " reserved");
        }
        quantityReserved -= quantity;
    }

    public void fulfil(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InsufficientStockException(
                    "Only " + quantityReserved + " units of " + productCode + " reserved");
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

    public String getProductCode() {
        return productCode;
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
