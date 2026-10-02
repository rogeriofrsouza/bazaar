package com.rogeriofrsouza.bazaar.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import io.hypersistence.tsid.TSID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.util.Assert;

public class Product {

    @Id
    private final Long id;

    private final String code;

    private final String name;

    private final @Nullable String description;

    private final BigDecimal price;

    private final Currency currency;

    private final @Nullable String imageUrl;

    private final ProductStatus status;

    private final Long categoryId;

    @Version
    private @Nullable Long version;

    @CreatedDate
    private @Nullable Instant createdAt;

    @LastModifiedDate
    private @Nullable Instant updatedAt;

    public Product(
            Long id,
            String code,
            String name,
            @Nullable String description,
            BigDecimal price,
            Currency currency,
            @Nullable String imageUrl,
            ProductStatus status,
            Long categoryId,
            @Nullable Instant createdAt,
            @Nullable Instant updatedAt
    ) {
        Assert.hasText(code, "Product code must not be blank");
        Assert.hasText(name, "Product name must not be blank");
        Assert.isTrue(price.signum() >= 0, "Product price must not be negative");
        this.id = id;
        this.code = code;
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
            String code,
            String name,
            @Nullable String description,
            BigDecimal price,
            Currency currency,
            @Nullable String imageUrl,
            Long categoryId
    ) {
        return new Product(
                TSID.Factory.getTsid().toLong(), code, name, description, price, currency, imageUrl,
                ProductStatus.ACTIVE, categoryId, null, null);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
