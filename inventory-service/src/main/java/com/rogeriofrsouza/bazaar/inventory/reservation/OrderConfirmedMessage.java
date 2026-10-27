package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.UUID;

public record OrderConfirmedMessage(UUID orderId) {

    public static final String ROUTING_KEY = "order.confirmed";
    public static final String QUEUE = "inventory-service.order-confirmed";
}
