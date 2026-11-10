package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class StockMessageListener {

    private final OrderService orderService;

    StockMessageListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = InventoryTopics.RESERVED, groupId = InventoryTopics.RESERVED_GROUP)
    void onStockReserved(StockReservedMessage message) {
        orderService.markReserved(message);
    }

    @KafkaListener(topics = InventoryTopics.REJECTED, groupId = InventoryTopics.REJECTED_GROUP)
    void onStockRejected(StockRejectedMessage message) {
        orderService.markRejected(message);
    }
}
