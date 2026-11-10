package com.rogeriofrsouza.bazaar.inventory.reservation;

public final class OrdersTopics {

    public static final String PLACED = "bazaar.orders.order-placed";
    public static final String PLACED_GROUP = "inventory-service.order-placed";

    public static final String CONFIRMED = "bazaar.orders.order-confirmed";
    public static final String CONFIRMED_GROUP = "inventory-service.order-confirmed";

    public static final String CANCELLED = "bazaar.orders.order-cancelled";
    public static final String CANCELLED_GROUP = "inventory-service.order-cancelled";

    private OrdersTopics() {
    }
}
