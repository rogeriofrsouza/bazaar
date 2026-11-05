package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class StockMessageListener {

    private final OrderService orderService;

    StockMessageListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = InventoryExchange.RESERVED_QUEUE)
    void onStockReserved(StockReservedMessage message) {
        orderService.markReserved(message);
    }

    @RabbitListener(queues = InventoryExchange.REJECTED_QUEUE)
    void onStockRejected(StockRejectedMessage message) {
        orderService.markRejected(message);
    }
}
