package com.rogeriofrsouza.bazaar.inventory.reservation;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class OrderMessageListener {

    private final StockReservationService stockReservationService;

    OrderMessageListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RabbitListener(queues = OrderPlacedMessage.QUEUE)
    void onOrderPlaced(OrderPlacedMessage message) {
        stockReservationService.reserve(message);
    }

    @RabbitListener(queues = OrderConfirmedMessage.QUEUE)
    void onOrderConfirmed(OrderConfirmedMessage message) {
        stockReservationService.fulfil(message);
    }

    @RabbitListener(queues = OrderCancelledMessage.QUEUE)
    void onOrderCancelled(OrderCancelledMessage message) {
        stockReservationService.release(message);
    }
}
