package com.rogeriofrsouza.bazaar.catalog.product;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    @Nullable String description,
    BigDecimal price,
    String currency,
    @Nullable String imageUrl
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCurrency().getCurrencyCode(),
            product.getImageUrl()
        );
    }
}
