package com.rogeriofrsouza.bazaar.order.order;

import java.util.UUID;

public record OrderCancelledMessage(UUID orderId) {
}
