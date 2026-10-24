package com.rogeriofrsouza.bazaar.order.inventory;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class StockReservedListener {

    private final InventoryClient inventoryClient;

    StockReservedListener(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    void release(StockReservedEvent event) {
        inventoryClient.release(event.reservation());
    }
}
