package com.rogeriofrsouza.bazaar.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String number,
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
                order.getNumber(),
                order.getStatus(),
                order.getCurrency().getCurrencyCode(),
                order.getTotal(),
                order.getCreatedAt(),
                items
        );
    }

    public record OrderItemResponse(
            String productCode,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductCode(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
            );
        }
    }
}
