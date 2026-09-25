package com.rogeriofrsouza.bazaar.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.rogeriofrsouza.bazaar.catalog.category.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private ProductCode code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private Currency currency;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    private Product(
        ProductCode code,
        String name,
        String description,
        BigDecimal price,
        Currency currency,
        String imageUrl,
        ProductStatus status,
        Category category
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.imageUrl = imageUrl;
        this.status = status;
        this.category = category;
    }

     public static Product create(
         ProductCode code,
         String name,
         String description,
         BigDecimal price,
         Currency currency,
         String imageUrl,
         Category category
     ) {
        return new Product(code, name, description, price, currency, imageUrl, ProductStatus.ACTIVE, category);
    }

    public Long getId() {
        return id;
    }

    public ProductCode getCode() {
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

    public Category getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
