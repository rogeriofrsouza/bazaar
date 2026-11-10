package com.rogeriofrsouza.bazaar.inventory.reservation;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderMessageListener {

    private final StockReservationService stockReservationService;

    OrderMessageListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @KafkaListener(topics = OrdersTopics.PLACED, groupId = OrdersTopics.PLACED_GROUP)
    void onOrderPlaced(OrderPlacedMessage message) {
        stockReservationService.reserve(message);
    }

    @KafkaListener(topics = OrdersTopics.CONFIRMED, groupId = OrdersTopics.CONFIRMED_GROUP)
    void onOrderConfirmed(OrderConfirmedMessage message) {
        stockReservationService.fulfil(message);
    }

    @KafkaListener(topics = OrdersTopics.CANCELLED, groupId = OrdersTopics.CANCELLED_GROUP)
    void onOrderCancelled(OrderCancelledMessage message) {
        stockReservationService.release(message);
    }
}
