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

    private static String subjectOf(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has no subject");
        }
        return subject;
    }
}
