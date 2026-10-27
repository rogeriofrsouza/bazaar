package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.UUID;

public record StockRejectedMessage(UUID orderId, String reason) {

    public static final String ROUTING_KEY = "stock.rejected";
}
