package com.rogeriofrsouza.bazaar.catalog.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    @Query("""
            select c from Category c
                left join fetch c.children ch
            where c.parent is null
            order by c.name, ch.name
            """)
    List<Category> findWithChildren();
}
