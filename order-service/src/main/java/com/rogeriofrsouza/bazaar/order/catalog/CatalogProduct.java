package com.rogeriofrsouza.bazaar.order.catalog;

import io.hypersistence.tsid.TSID;

import java.math.BigDecimal;
import java.util.Currency;

public record CatalogProduct(
        TSID id,
        String name,
        BigDecimal price,
        Currency currency
) {
}
