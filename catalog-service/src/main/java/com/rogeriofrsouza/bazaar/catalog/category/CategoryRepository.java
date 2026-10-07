package com.rogeriofrsouza.bazaar.catalog.category;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends ListCrudRepository<Category, UUID>,
        ListPagingAndSortingRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);
}
