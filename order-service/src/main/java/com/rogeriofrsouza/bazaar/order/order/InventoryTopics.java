package com.rogeriofrsouza.bazaar.order.order;

public final class InventoryTopics {

    public static final String RESERVED = "bazaar.inventory.stock-reserved";
    public static final String RESERVED_GROUP = "order-service.stock-reserved";

    public static final String REJECTED = "bazaar.inventory.stock-rejected";
    public static final String REJECTED_GROUP = "order-service.stock-rejected";

    private InventoryTopics() {
    }
}
