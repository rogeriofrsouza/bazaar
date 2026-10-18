package com.rogeriofrsouza.bazaar.customer.customer;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class CustomerTests {

    @Test
    void createGeneratesUuidV7Id() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);

        assertThat(customer.getId().version()).isEqualTo(7);
        assertThat(Instant.ofEpochMilli(customer.getId().getMostSignificantBits() >>> 16))
                .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(customer.getSubject()).isEqualTo("subject-1");
        assertThat(customer.getFirstName()).isEqualTo("Ada");
        assertThat(customer.getLastName()).isEqualTo("Lovelace");
        assertThat(customer.getEmail()).isEqualTo("ada@example.com");
        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getCreatedAt()).isNull();
    }

    @Test
    void rejectsBlankSubject() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Customer.create(" ", "Ada", "Lovelace", "ada@example.com", null))
                .withMessage("Subject must not be blank");
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Customer.create("subject-1", " ", "Lovelace", "ada@example.com", null))
                .withMessage("First name must not be blank");
    }

    @Test
    void updateChangesProfile() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);

        customer.update("Augusta", "King", "augusta@example.com", "+44 20 7946 0000");

        assertThat(customer.getFirstName()).isEqualTo("Augusta");
        assertThat(customer.getLastName()).isEqualTo("King");
        assertThat(customer.getEmail()).isEqualTo("augusta@example.com");
        assertThat(customer.getPhone()).isEqualTo("+44 20 7946 0000");
    }

    @Test
    void updateRejectsBlankEmail() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> customer.update("Ada", "Lovelace", "", null))
                .withMessage("Email must not be blank");
        assertThat(customer.getEmail()).isEqualTo("ada@example.com");
    }
}
