package com.rogeriofrsouza.bazaar.order.order;

import java.util.List;
import java.util.UUID;

public record OrderPlacedMessage(UUID orderId, List<OrderPlacedItem> items) {

    public static OrderPlacedMessage from(Order order) {
        List<OrderPlacedItem> items = order.getItems()
                .stream()
                .map(item -> new OrderPlacedItem(item.getProductId(), item.getQuantity()))
                .toList();

        return new OrderPlacedMessage(order.getId(), items);
    }

    public record OrderPlacedItem(UUID productId, int quantity) {
    }
}
