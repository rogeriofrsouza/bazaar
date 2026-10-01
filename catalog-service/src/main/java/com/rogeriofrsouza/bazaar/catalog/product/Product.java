package com.rogeriofrsouza.bazaar.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

public class Product {

    @Id
    private Long id;

    private String code;

    private String name;

    private String description;

    private BigDecimal price;

    private Currency currency;

    private String imageUrl;

    private ProductStatus status;

    private Long categoryId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Product() {
    }

    private Product(
        String code,
        String name,
        String description,
        BigDecimal price,
        Currency currency,
        String imageUrl,
        ProductStatus status,
        Long categoryId
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.imageUrl = imageUrl;
        this.status = status;
        this.categoryId = categoryId;
    }

    public static Product create(
        String code,
        String name,
        String description,
        BigDecimal price,
        Currency currency,
        String imageUrl,
        Long categoryId
    ) {
        return new Product(code, name, description, price, currency, imageUrl, ProductStatus.ACTIVE, categoryId);
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

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
