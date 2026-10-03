package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

final class InventoryItemSpecifications {

    private InventoryItemSpecifications() {
    }

    static Specification<InventoryItem> productIdIn(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, _, _) -> root.get("productId").in(productIds);
    }
}
