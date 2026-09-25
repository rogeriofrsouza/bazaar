package com.rogeriofrsouza.bazaar.catalog.product;

import java.util.Objects;
import java.util.regex.Pattern;

public record ProductCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z]{2}\\d{2}[A-Za-z]{4}");

    public ProductCode {
        Objects.requireNonNull(value, "Product code must not be null");

        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid product code: " + value);
        }
    }
}
