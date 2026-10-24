package com.rogeriofrsouza.bazaar.order.inventory;

public record StockReservedEvent(
        ReservationRequest reservation
) {
}
