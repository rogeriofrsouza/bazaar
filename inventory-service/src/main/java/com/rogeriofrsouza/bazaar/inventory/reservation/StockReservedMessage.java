package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.UUID;

public record StockReservedMessage(UUID orderId) {

    public static final String ROUTING_KEY = "stock.reserved";
}
