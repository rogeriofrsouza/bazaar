package com.rogeriofrsouza.bazaar.catalog.product;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record ProductCode(String value) {

    public static final String REGEX = "[A-Za-z]{2}\\d{2}[A-Za-z]{4}";

    private static final Pattern FORMAT = Pattern.compile(REGEX);

    public ProductCode {
        Objects.requireNonNull(value, "Product code must not be null");
        value = value.toUpperCase(Locale.ROOT);

        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid product code: " + value);
        }
    }
}
