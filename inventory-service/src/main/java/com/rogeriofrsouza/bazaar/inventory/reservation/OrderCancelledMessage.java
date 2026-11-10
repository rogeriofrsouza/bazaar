package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.UUID;

public record OrderCancelledMessage(UUID orderId) {
}
