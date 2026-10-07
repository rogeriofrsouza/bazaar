package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreateInventoryItemRequest(
        @NotNull
        UUID productId,

        @NotNull
        @PositiveOrZero
        Integer quantityOnHand
) {
}
