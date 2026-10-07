package com.rogeriofrsouza.bazaar.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.util.Assert;

public class Product {

    @Id
    private final UUID id;

    private final String name;

    private final @Nullable String description;

    private final BigDecimal price;

    private final Currency currency;

    private final @Nullable String imageUrl;

    private final ProductStatus status;

    private final UUID categoryId;

    @Version
    private @Nullable Long version;

    @CreatedDate
    private @Nullable Instant createdAt;

    @LastModifiedDate
    private @Nullable Instant updatedAt;

    public Product(
            UUID id,
            String name,
            @Nullable String description,
            BigDecimal price,
            Currency currency,
            @Nullable String imageUrl,
            ProductStatus status,
            UUID categoryId,
            @Nullable Instant createdAt,
            @Nullable Instant updatedAt
    ) {
        Assert.hasText(name, "Product name must not be blank");
        Assert.isTrue(price.signum() >= 0, "Product price must not be negative");
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.imageUrl = imageUrl;
        this.status = status;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(
            String name,
            @Nullable String description,
            BigDecimal price,
            Currency currency,
            @Nullable String imageUrl,
            UUID categoryId
    ) {
        return new Product(
                UUID.ofEpochMillis(System.currentTimeMillis()), name, description, price, currency, imageUrl,
                ProductStatus.ACTIVE, categoryId, null, null);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
