package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends ListCrudRepository<Order, UUID> {

    Optional<Order> findByIdAndCustomerSubject(UUID id, String customerSubject);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByIdAndCustomerSubject(UUID id, String customerSubject);

    Page<Order> findByCustomerSubject(String customerSubject, Pageable pageable);
}
