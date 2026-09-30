package com.rogeriofrsouza.bazaar.catalog.product;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> hasStatus(ProductStatus status) {
        return (root, _, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Product> inCategory(String slug) {
        if (slug == null) {
            return Specification.unrestricted();
        }
        return (root, _, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    static Specification<Product> codeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, _, _) -> root.get("code").in(codes);
    }
}
