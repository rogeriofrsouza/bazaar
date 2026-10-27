package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.List;
import java.util.UUID;

public record OrderPlacedMessage(UUID orderId, List<OrderPlacedItem> items) {

    public static final String ROUTING_KEY = "order.placed";
    public static final String QUEUE = "inventory-service.order-placed";

    public record OrderPlacedItem(UUID productId, int quantity) {
    }
}
