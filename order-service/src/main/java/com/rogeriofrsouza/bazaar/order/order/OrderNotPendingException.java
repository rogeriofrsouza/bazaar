package com.rogeriofrsouza.bazaar.order.order;

import java.util.UUID;

public class OrderNotPendingException extends RuntimeException {

    public OrderNotPendingException(UUID orderId, OrderStatus status) {
        super("Order " + orderId + " is " + status + ", not " + OrderStatus.PENDING);
    }
}
