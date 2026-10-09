package com.rogeriofrsouza.bazaar.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        String currency,
        BigDecimal total,
        Instant createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getCurrency().getCurrencyCode(),
                order.getTotal(),
                Objects.requireNonNull(order.getCreatedAt(), "Order must be saved before it is mapped"),
                items
        );
    }

    public record OrderItemResponse(
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
            );
        }
    }
}
