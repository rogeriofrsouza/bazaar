package com.rogeriofrsouza.bazaar.customer.customer;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.util.UUID;

public class Address {

    @Id
    private final UUID id;

    private final String line1;

    private final @Nullable String line2;

    private final String city;

    private final String state;

    private final String postalCode;

    private final String country;

    public Address(
            UUID id,
            String line1,
            @Nullable String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
        Assert.hasText(line1, "Address line 1 must not be blank");
        Assert.hasText(city, "City must not be blank");
        Assert.hasText(state, "State must not be blank");
        Assert.hasText(postalCode, "Postal code must not be blank");
        Assert.isTrue(country.length() == 2, "Country must be an ISO 3166-1 alpha-2 code");
        this.id = id;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public static Address create(
            String line1, @Nullable String line2, String city, String state, String postalCode, String country) {
        return new Address(
                UUID.ofEpochMillis(System.currentTimeMillis()), line1, line2, city, state, postalCode, country);
    }

    public UUID getId() {
        return id;
    }

    public String getLine1() {
        return line1;
    }

    public @Nullable String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
