package com.rogeriofrsouza.bazaar.order.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotBlank
        String productCode,

        @NotNull
        @Positive
        Integer quantity
) {
}
