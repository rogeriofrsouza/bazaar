package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class ProductMessageListener {

    private final InventoryItemService inventoryItemService;

    ProductMessageListener(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @RabbitListener(queues = CatalogExchange.PRODUCT_CREATED_QUEUE)
    void onProductCreated(ProductCreatedMessage message) {
        inventoryItemService.initialize(message.productId());
    }
}
