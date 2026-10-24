package com.rogeriofrsouza.bazaar.order.inventory;

import com.rogeriofrsouza.bazaar.order.order.OrderItem;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record ReservationRequest(
        List<ReservationItemRequest> items
) {
    public ReservationRequest(Collection<OrderItem> items) {
        this(items.stream()
                .map(item -> new ReservationItemRequest(item.getProductId(), item.getQuantity()))
                .toList());
    }

    public record ReservationItemRequest(
            UUID productId,
            int quantity
    ) {
    }
}
