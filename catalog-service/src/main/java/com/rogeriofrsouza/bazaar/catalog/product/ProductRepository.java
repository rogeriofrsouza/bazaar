package com.rogeriofrsouza.bazaar.catalog.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface ProductRepository extends ListCrudRepository<Product, UUID>,
        ListPagingAndSortingRepository<Product, UUID> {

    Optional<Product> findByIdAndStatus(UUID id, ProductStatus status);
}
