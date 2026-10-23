package com.rogeriofrsouza.bazaar.customer.customer;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.*;

public class Customer {

    @Id
    private final UUID id;

    private final String subject;

    private String firstName;

    private String lastName;

    private String email;

    private @Nullable String phone;

    @MappedCollection(idColumn = "customer_id")
    private final Set<Address> addresses;

    @Version
    private @Nullable Long version;

    @CreatedDate
    private @Nullable Instant createdAt;

    @LastModifiedDate
    private @Nullable Instant updatedAt;

    public Customer(
            UUID id,
            String subject,
            String firstName,
            String lastName,
            String email,
            @Nullable String phone,
            Set<Address> addresses,
            @Nullable Instant createdAt,
            @Nullable Instant updatedAt
    ) {
        Assert.hasText(subject, "Subject must not be blank");
        requireProfile(firstName, lastName, email);
        this.id = id;
        this.subject = subject;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.addresses = new HashSet<>(addresses);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Customer create(
            String subject, String firstName, String lastName, String email, @Nullable String phone) {
        return new Customer(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                subject,
                firstName,
                lastName,
                email,
                phone,
                Set.of(),
                null,
                null
        );
    }

    public void update(String firstName, String lastName, String email, @Nullable String phone) {
        requireProfile(firstName, lastName, email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public Optional<Address> findAddress(UUID addressId) {
        return addresses.stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst();
    }

    public Address addAddress(String line1,
                              @Nullable String line2,
                              String city,
                              String state,
                              String postalCode,
                              String country) {
        Address address = Address.create(line1, line2, city, state, postalCode, country);
        addresses.add(address);
        return address;
    }

    public Address updateAddress(UUID addressId,
                                 String line1,
                                 @Nullable String line2,
                                 String city,
                                 String state,
                                 String postalCode,
                                 String country) {
        Address address = new Address(addressId, line1, line2, city, state, postalCode, country);
        removeAddress(addressId);
        addresses.add(address);
        return address;
    }

    public void removeAddress(UUID addressId) {
        if (!addresses.removeIf(address -> address.getId().equals(addressId))) {
            throw new AddressNotFoundException(addressId);
        }
    }

    private static void requireProfile(String firstName, String lastName, String email) {
        Assert.hasText(firstName, "First name must not be blank");
        Assert.hasText(lastName, "Last name must not be blank");
        Assert.hasText(email, "Email must not be blank");
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public @Nullable String getPhone() {
        return phone;
    }

    public Set<Address> getAddresses() {
        return Collections.unmodifiableSet(addresses);
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
