package com.rogeriofrsouza.bazaar.catalog.product;

import jakarta.validation.constraints.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Currency;

public record CreateProductRequest(
    @NotBlank
    @Size(max = 200)
    String name,

    @Nullable String description,

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    BigDecimal price,

    @NotNull
    Currency currency,

    @Size(max = 255)
    @Nullable String imageUrl,

    @NotBlank
    String category
) {
}
