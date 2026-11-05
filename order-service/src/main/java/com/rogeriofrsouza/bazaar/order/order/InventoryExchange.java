package com.rogeriofrsouza.bazaar.order.order;

public final class InventoryExchange {

    public static final String NAME = "bazaar.inventory";

    public static final String RESERVED = "stock.reserved";
    public static final String RESERVED_QUEUE = "order-service.stock-reserved";

    public static final String REJECTED = "stock.rejected";
    public static final String REJECTED_QUEUE = "order-service.stock-rejected";

    private InventoryExchange() {
    }
}
