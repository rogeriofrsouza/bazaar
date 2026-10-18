package com.rogeriofrsouza.bazaar.customer;

import com.rogeriofrsouza.bazaar.customer.customer.CreateCustomerRequest;
import com.rogeriofrsouza.bazaar.customer.customer.Customer;
import com.rogeriofrsouza.bazaar.customer.customer.CustomerRepository;
import com.rogeriofrsouza.bazaar.customer.customer.CustomerResponse;
import com.rogeriofrsouza.bazaar.customer.customer.CustomerService;
import com.rogeriofrsouza.bazaar.customer.customer.UpdateCustomerRequest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Import(ContainersConfig.class)
@Transactional
class CustomerPersistenceTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAndReadsProfile() {
        String subject = subject();

        CustomerResponse created = customerService.create(subject, createRequest("+55 11 91234-5678"));
        assertThat(created.firstName()).isEqualTo("Ada");
        assertThat(created.lastName()).isEqualTo("Lovelace");
        assertThat(created.email()).isEqualTo("ada@example.com");
        assertThat(created.phone()).isEqualTo("+55 11 91234-5678");
        assertThat(customerService.findBySubject(subject)).isEqualTo(created);

        Customer customer = customerRepository.findBySubject(subject).orElseThrow();
        assertThat(customer.getId()).isEqualTo(created.id());
        assertThat(customer.getId().version()).isEqualTo(7);
        assertThat(customer.getSubject()).isEqualTo(subject);
        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getUpdatedAt()).isNotNull();
        assertThat(versionOf(subject)).isZero();
    }

    @Test
    void createsProfileWithoutPhone() {
        String subject = subject();

        customerService.create(subject, createRequest(null));

        assertThat(customerService.findBySubject(subject).phone()).isNull();
    }

    @Test
    void rejectsSecondProfileForSameSubject() {
        String subject = subject();
        customerService.create(subject, createRequest(null));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> customerService.create(subject, createRequest(null)))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void persistsProfileChanges() {
        String subject = subject();
        CustomerResponse created = customerService.create(subject, createRequest("+55 11 91234-5678"));
        Customer before = customerRepository.findBySubject(subject).orElseThrow();

        CustomerResponse updated = customerService.update(
                subject, new UpdateCustomerRequest("Augusta", "King", "augusta@example.com", null));

        assertThat(updated)
                .isEqualTo(new CustomerResponse(created.id(), "Augusta", "King", "augusta@example.com", null));
        assertThat(customerService.findBySubject(subject)).isEqualTo(updated);

        Customer after = customerRepository.findBySubject(subject).orElseThrow();
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(after.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
        assertThat(versionOf(subject)).isEqualTo(1);
    }

    @Test
    void failsForSubjectWithoutProfile() {
        String subject = subject();

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> customerService.findBySubject(subject))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> customerService.update(
                        subject, new UpdateCustomerRequest("Ada", "Lovelace", "ada@example.com", null)))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static String subject() {
        return UUID.randomUUID().toString();
    }

    private static CreateCustomerRequest createRequest(@Nullable String phone) {
        return new CreateCustomerRequest("Ada", "Lovelace", "ada@example.com", phone);
    }

    private Long versionOf(String subject) {
        return jdbcTemplate.queryForObject("SELECT version FROM customer WHERE subject = ?", Long.class, subject);
    }
}
