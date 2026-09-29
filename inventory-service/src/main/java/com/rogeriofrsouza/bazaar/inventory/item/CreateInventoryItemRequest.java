package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateInventoryItemRequest(
        @NotBlank
        String productCode,

        @NotNull
        @PositiveOrZero
        Integer quantityOnHand
) {
}
