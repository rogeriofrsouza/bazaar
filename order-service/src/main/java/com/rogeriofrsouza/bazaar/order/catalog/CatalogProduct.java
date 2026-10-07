package com.rogeriofrsouza.bazaar.order.catalog;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public record CatalogProduct(
        UUID id,
        String name,
        BigDecimal price,
        Currency currency
) {
}
