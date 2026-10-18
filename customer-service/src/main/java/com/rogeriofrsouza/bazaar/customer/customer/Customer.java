package com.rogeriofrsouza.bazaar.customer.customer;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

public class Customer {

    @Id
    private final UUID id;

    private final String subject;

    private String firstName;

    private String lastName;

    private String email;

    private @Nullable String phone;

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

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
