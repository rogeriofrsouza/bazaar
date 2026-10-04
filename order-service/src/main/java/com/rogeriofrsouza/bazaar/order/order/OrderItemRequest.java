package com.rogeriofrsouza.bazaar.order.order;

import io.hypersistence.tsid.TSID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull
        TSID productId,

        @NotNull
        @Positive
        Integer quantity
) {
}
