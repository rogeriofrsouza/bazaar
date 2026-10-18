package com.rogeriofrsouza.bazaar.customer.customer;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends ListCrudRepository<Customer, UUID> {

    Optional<Customer> findBySubject(String subject);

    boolean existsBySubject(String subject);
}
