package com.rogeriofrsouza.bazaar.order.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty
        @Size(max = 10)
        List<@Valid OrderItemRequest> items
) {
}
