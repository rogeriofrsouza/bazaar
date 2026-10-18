package com.rogeriofrsouza.bazaar.customer.customer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResponse findBySubject(String subject) {
        return CustomerResponse.from(find(subject));
    }

    @Transactional
    public CustomerResponse create(String subject, CreateCustomerRequest request) {
        if (customerRepository.existsBySubject(subject)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer profile already exists");
        }

        Customer customer = Customer.create(
                subject, request.firstName(), request.lastName(), request.email(), request.phone());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(String subject, UpdateCustomerRequest request) {
        Customer customer = find(subject);
        customer.update(request.firstName(), request.lastName(), request.email(), request.phone());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    private Customer find(String subject) {
        return customerRepository.findBySubject(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }
}
