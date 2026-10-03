package com.rogeriofrsouza.bazaar.inventory.item;

import io.hypersistence.tsid.TSID;

public record InventoryItemResponse(
    String productId,
    int quantityOnHand,
    int quantityReserved,
    int available
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
            TSID.from(item.getProductId()).toString(),
            item.getQuantityOnHand(),
            item.getQuantityReserved(),
            item.getAvailable()
        );
    }
}
