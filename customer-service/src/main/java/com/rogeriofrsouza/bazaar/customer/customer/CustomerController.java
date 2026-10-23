package com.rogeriofrsouza.bazaar.customer.customer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/customers")
@RestController
class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    CustomerResponse get(@AuthenticationPrincipal Jwt jwt) {
        return customerService.findBySubject(subjectOf(jwt));
    }

    @PostMapping("/me")
    ResponseEntity<CustomerResponse> create(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse customer = customerService.create(subjectOf(jwt), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .toUri();

        return ResponseEntity.created(location).body(customer);
    }

    @PutMapping("/me")
    CustomerResponse update(@AuthenticationPrincipal Jwt jwt,
                            @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.update(subjectOf(jwt), request);
    }

    @GetMapping("/me/addresses")
    List<AddressResponse> listAddresses(@AuthenticationPrincipal Jwt jwt) {
        return customerService.listAddresses(subjectOf(jwt));
    }

    @GetMapping("/me/addresses/{addressId}")
    AddressResponse getAddress(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable UUID addressId) {
        return customerService.findAddress(subjectOf(jwt), addressId);
    }

    @PostMapping("/me/addresses")
    ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = customerService.addAddress(subjectOf(jwt), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(address.id())
                .toUri();

        return ResponseEntity.created(location).body(address);
    }

    @PutMapping("/me/addresses/{addressId}")
    AddressResponse updateAddress(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID addressId,
                                  @Valid @RequestBody UpdateAddressRequest request) {
        return customerService.updateAddress(subjectOf(jwt), addressId, request);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeAddress(@AuthenticationPrincipal Jwt jwt,
                       @PathVariable UUID addressId) {
        customerService.removeAddress(subjectOf(jwt), addressId);
    }

    private static String subjectOf(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has no subject");
        }
        return subject;
    }
}
