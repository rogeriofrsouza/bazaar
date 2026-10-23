package com.rogeriofrsouza.bazaar.customer.customer;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(UUID addressId) {
        super("Address " + addressId + " not found");
    }
}
