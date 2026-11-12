package com.rogeriofrsouza.bazaar.inventory.reservation;

import java.util.UUID;

public record OrderExpiredMessage(UUID orderId) {
}
