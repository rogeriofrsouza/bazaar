package com.rogeriofrsouza.bazaar.catalog.product;

import java.math.BigDecimal;

public record ProductResponse(
    String code,
    String name,
    String description,
    BigDecimal price,
    String currency,
    String imageUrl
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getCode().value(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCurrency().getCurrencyCode(),
            product.getImageUrl()
        );
    }
}
