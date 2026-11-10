package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.List;
import java.util.UUID;

public record OrderPlacedMessage(UUID orderId, List<OrderPlacedItem> items) {

    public record OrderPlacedItem(UUID productId, int quantity) {
    }
}
