package com.rogeriofrsouza.bazaar.catalog.product;

import io.hypersistence.tsid.TSID;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record ProductResponse(
    String id,
    String name,
    @Nullable String description,
    BigDecimal price,
    String currency,
    @Nullable String imageUrl
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            TSID.from(product.getId()).toString(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCurrency().getCurrencyCode(),
            product.getImageUrl()
        );
    }
}
