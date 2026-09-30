package com.rogeriofrsouza.bazaar.order.catalog;

import java.math.BigDecimal;
import java.util.Currency;

public record CatalogProduct(
        String code,
        String name,
        BigDecimal price,
        Currency currency
) {
}
