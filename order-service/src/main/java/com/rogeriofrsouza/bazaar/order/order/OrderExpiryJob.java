package com.rogeriofrsouza.bazaar.order.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderService orderService;

    OrderExpiryJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${bazaar.orders.expiry-check-delay}")
    void expireOverdueOrders() {
        try {
            orderService.expireOverdueOrders();
        } catch (RuntimeException ex) {
            log.warn("Could not expire overdue orders", ex);
        }
    }
}
