package com.rogeriofrsouza.bazaar.order.order;

import java.util.UUID;

public record OrderExpiredMessage(UUID orderId) {
}
