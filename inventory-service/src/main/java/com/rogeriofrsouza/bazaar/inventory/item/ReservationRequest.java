package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(
        @NotEmpty
        @Size(max = 20)
        List<@Valid ReservationItemRequest> items
) {
    public record ReservationItemRequest(
            @NotNull
            UUID productId,

            @NotNull
            @Positive
            Integer quantity
    ) {
    }
}
