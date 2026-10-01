package com.rogeriofrsouza.bazaar.catalog.product;

import org.springframework.data.relational.core.query.Criteria;

import java.util.Collection;

final class ProductCriteria {

    private ProductCriteria() {
    }

    static Criteria hasStatus(ProductStatus status) {
        return Criteria.where("status").is(status);
    }

    static Criteria inCategory(Long categoryId) {
        if (categoryId == null) {
            return Criteria.empty();
        }
        return Criteria.where("categoryId").is(categoryId);
    }

    static Criteria codeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Criteria.empty();
        }
        return Criteria.where("code").in(codes);
    }
}
