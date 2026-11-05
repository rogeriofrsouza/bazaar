package com.rogeriofrsouza.bazaar.order.order;

import java.util.UUID;

public class OrderNotReservedException extends RuntimeException {

    public OrderNotReservedException(UUID orderId, OrderStatus status) {
        super("Order " + orderId + " is " + status + ", not " + OrderStatus.RESERVED);
    }
}
