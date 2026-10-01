package com.rogeriofrsouza.bazaar.catalog.category;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

import java.util.Optional;

public interface CategoryRepository extends ListCrudRepository<Category, Long>,
        ListPagingAndSortingRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
}
