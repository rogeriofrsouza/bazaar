package com.rogeriofrsouza.bazaar.customer.customer;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String line1,
        @Nullable String line2,
        String city,
        String state,
        String postalCode,
        String country
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}
