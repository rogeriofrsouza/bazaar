package com.rogeriofrsouza.bazaar.catalog.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByCodeAndStatus(String code, ProductStatus status);

    boolean existsByCode(String code);
}
