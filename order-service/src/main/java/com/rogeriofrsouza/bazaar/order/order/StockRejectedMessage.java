package com.rogeriofrsouza.bazaar.order.order;

import java.util.UUID;

public record StockRejectedMessage(UUID orderId, String reason) {
}
