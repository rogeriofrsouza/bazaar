package com.rogeriofrsouza.bazaar.inventory.item;

import java.util.UUID;

public record InventoryItemResponse(
    UUID productId,
    int quantityOnHand,
    int quantityReserved,
    int available
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
            item.getProductId(),
            item.getQuantityOnHand(),
            item.getQuantityReserved(),
            item.getAvailable()
        );
    }
}
