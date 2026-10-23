package com.rogeriofrsouza.bazaar.customer.customer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(String subject) {
        return find(subject).getAddresses()
                .stream()
                .sorted(Comparator.comparing(Address::getId))
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse findAddress(String subject, UUID addressId) {
        Customer customer = find(subject);
        return customer.findAddress(addressId)
                .map(AddressResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    @Transactional
    public AddressResponse addAddress(String subject, CreateAddressRequest request) {
        Customer customer = find(subject);

        Address address = customer.addAddress(
                request.line1(), request.line2(), request.city(), request.state(), request.postalCode(), request.country()
        );

        customerRepository.save(customer);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse updateAddress(String subject, UUID addressId, UpdateAddressRequest request) {
        Customer customer = find(subject);
        customer.findAddress(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        Address address = customer.updateAddress(
                addressId, request.line1(), request.line2(), request.city(),
                request.state(), request.postalCode(), request.country()
        );

        customerRepository.save(customer);
        return AddressResponse.from(address);
    }

    @Transactional
    public void removeAddress(String subject, UUID addressId) {
        Customer customer = find(subject);
        customer.findAddress(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        customer.removeAddress(addressId);
        customerRepository.save(customer);
    }

    private Customer find(String subject) {
        return customerRepository.findBySubject(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }
}
