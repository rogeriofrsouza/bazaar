package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

final class InventoryItemSpecifications {

    private InventoryItemSpecifications() {
    }

    static Specification<InventoryItem> productCodeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, _, _) -> root.get("productCode").in(codes);
    }
}
