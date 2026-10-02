package com.rogeriofrsouza.bazaar.catalog.product;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface ProductRepository extends ListCrudRepository<Product, Long>,
        ListPagingAndSortingRepository<Product, Long> {

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);
}
