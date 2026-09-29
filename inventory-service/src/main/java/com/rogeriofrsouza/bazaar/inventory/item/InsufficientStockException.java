package com.rogeriofrsouza.bazaar.inventory.item;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
