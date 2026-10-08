package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.relational.core.query.Criteria;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.UUID;

final class InventoryItemCriteria {

    private InventoryItemCriteria() {
    }

    static Criteria productIdIn(Collection<UUID> productIds) {
        return CollectionUtils.isEmpty(productIds)
                ? Criteria.empty()
                : Criteria.where("productId").in(productIds);
    }
}
