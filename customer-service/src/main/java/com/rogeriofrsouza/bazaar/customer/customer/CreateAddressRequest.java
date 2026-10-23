package com.rogeriofrsouza.bazaar.customer.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateAddressRequest(
        @NotBlank
        @Size(max = 200)
        String line1,

        @Size(max = 200)
        @Nullable String line2,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 100)
        String state,

        @NotBlank
        @Size(max = 20)
        String postalCode,

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}")
        String country
) {
}
