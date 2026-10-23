package com.rogeriofrsouza.bazaar.customer.customer;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    @Test
    void addAddressGeneratesUuidV7Id() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);

        Address address = customer.addAddress("221B Baker Street", null, "London", "England", "NW1 6XE", "GB");

        assertThat(address.getId().version()).isEqualTo(7);
        assertThat(customer.getAddresses()).containsExactly(address);
        assertThat(customer.findAddress(address.getId())).containsSame(address);
    }

    @Test
    void addAddressRejectsBlankLine1() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> customer.addAddress(" ", null, "London", "England", "NW1 6XE", "GB"))
                .withMessage("Address line 1 must not be blank");
        assertThat(customer.getAddresses()).isEmpty();
    }

    @Test
    void addAddressRejectsCountryThatIsNotTwoLetters() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> customer.addAddress(
                        "221B Baker Street", null, "London", "England", "NW1 6XE", "GBR"))
                .withMessage("Country must be an ISO 3166-1 alpha-2 code");
    }

    @Test
    void updateAddressKeepsId() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);
        Address address = customer.addAddress("221B Baker Street", null, "London", "England", "NW1 6XE", "GB");

        Address updated = customer.updateAddress(
                address.getId(), "10 Downing Street", "Flat 1", "London", "England", "SW1A 2AA", "GB");

        assertThat(updated.getId()).isEqualTo(address.getId());
        assertThat(updated.getLine1()).isEqualTo("10 Downing Street");
        assertThat(updated.getLine2()).isEqualTo("Flat 1");
        assertThat(updated.getPostalCode()).isEqualTo("SW1A 2AA");
        assertThat(customer.getAddresses()).containsExactly(updated);
    }

    @Test
    void removeAddressDropsIt() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);
        Address address = customer.addAddress("221B Baker Street", null, "London", "England", "NW1 6XE", "GB");

        customer.removeAddress(address.getId());

        assertThat(customer.getAddresses()).isEmpty();
        assertThat(customer.findAddress(address.getId())).isEmpty();
    }

    @Test
    void updateAddressRejectsUnknownId() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);
        Address address = customer.addAddress("221B Baker Street", null, "London", "England", "NW1 6XE", "GB");
        UUID unknownId = UUID.randomUUID();

        assertThatExceptionOfType(AddressNotFoundException.class)
                .isThrownBy(() -> customer.updateAddress(
                        unknownId, "10 Downing Street", null, "London", "England", "SW1A 2AA", "GB"))
                .withMessage("Address " + unknownId + " not found");
        assertThat(customer.getAddresses()).containsExactly(address);
    }

    @Test
    void removeAddressRejectsUnknownId() {
        Customer customer = Customer.create("subject-1", "Ada", "Lovelace", "ada@example.com", null);
        Address address = customer.addAddress("221B Baker Street", null, "London", "England", "NW1 6XE", "GB");

        assertThatExceptionOfType(AddressNotFoundException.class)
                .isThrownBy(() -> customer.removeAddress(UUID.randomUUID()));
        assertThat(customer.getAddresses()).containsExactly(address);
    }
}
