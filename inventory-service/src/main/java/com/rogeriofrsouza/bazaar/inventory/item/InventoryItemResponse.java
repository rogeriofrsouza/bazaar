package com.rogeriofrsouza.bazaar.inventory.item;

public record InventoryItemResponse(
    String productCode,
    int quantityOnHand,
    int quantityReserved,
    int available
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
            item.getProductCode(),
            item.getQuantityOnHand(),
            item.getQuantityReserved(),
            item.getAvailable()
        );
    }
}
