package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.UUID;

final class InventoryItemSpecifications {

    private InventoryItemSpecifications() {
    }

    static Specification<InventoryItem> productIdIn(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, _, _) -> root.get("productId").in(productIds);
    }
}
