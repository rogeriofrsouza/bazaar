package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuantityRequest(
        @NotNull
        @Positive
        Integer quantity
) {
}
