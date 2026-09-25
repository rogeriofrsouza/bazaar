package com.rogeriofrsouza.bazaar.catalog.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndCategorySlug(ProductStatus status, String slug, Pageable pageable);

    Optional<Product> findByCodeAndStatus(ProductCode code, ProductStatus status);
}
