package com.rogeriofrsouza.bazaar.catalog.product;

import org.jspecify.annotations.Nullable;
import org.springframework.data.relational.core.query.Criteria;

import java.util.Collection;

final class ProductCriteria {

    private ProductCriteria() {
    }

    static Criteria hasStatus(ProductStatus status) {
        return Criteria.where("status").is(status);
    }

    static Criteria inCategory(@Nullable Long categoryId) {
        if (categoryId == null) {
            return Criteria.empty();
        }
        return Criteria.where("categoryId").is(categoryId);
    }

    static Criteria idIn(@Nullable Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Criteria.empty();
        }
        return Criteria.where("id").in(ids);
    }
}
