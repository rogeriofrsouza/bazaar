package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends ListCrudRepository<Order, UUID> {

    Optional<Order> findByIdAndCustomerSubject(UUID id, String customerSubject);
}
