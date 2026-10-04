package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select o from Order o
                left join fetch o.items
            where o.id = :id
            """)
    Optional<Order> findWithItemsById(Long id);
}
