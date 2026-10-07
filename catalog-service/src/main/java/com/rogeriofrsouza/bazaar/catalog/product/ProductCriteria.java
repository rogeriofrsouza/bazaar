package com.rogeriofrsouza.bazaar.catalog.product;

import org.jspecify.annotations.Nullable;
import org.springframework.data.relational.core.query.Criteria;

import java.util.Collection;
import java.util.UUID;

final class ProductCriteria {

    private ProductCriteria() {
    }

    static Criteria hasStatus(ProductStatus status) {
        return Criteria.where("status").is(status);
    }

    static Criteria inCategory(@Nullable UUID categoryId) {
        if (categoryId == null) {
            return Criteria.empty();
        }
        return Criteria.where("categoryId").is(categoryId);
    }

    static Criteria idIn(@Nullable Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Criteria.empty();
        }
        return Criteria.where("id").in(ids);
    }
}
