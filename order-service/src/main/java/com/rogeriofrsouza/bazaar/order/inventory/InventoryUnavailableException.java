package com.rogeriofrsouza.bazaar.order.inventory;

public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(Throwable cause) {
        super("Inventory service failed", cause);
    }
}
