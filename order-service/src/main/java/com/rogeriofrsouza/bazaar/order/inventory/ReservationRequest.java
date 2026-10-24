package com.rogeriofrsouza.bazaar.order.inventory;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(
        List<ReservationItemRequest> items
) {
    public record ReservationItemRequest(
            UUID productId,
            int quantity
    ) {
    }
}
