package com.rogeriofrsouza.bazaar.inventory.item;

import io.hypersistence.tsid.TSID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateInventoryItemRequest(
        @NotNull
        TSID productId,

        @NotNull
        @PositiveOrZero
        Integer quantityOnHand
) {
}
