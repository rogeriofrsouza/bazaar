package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.UUID;

public record OrderCancelledMessage(UUID orderId) {

    public static final String ROUTING_KEY = "order.cancelled";
    public static final String QUEUE = "inventory-service.order-cancelled";
}
