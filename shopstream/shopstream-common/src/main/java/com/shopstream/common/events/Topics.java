package com.shopstream.common.events;

/**
 * Kafka topic names shared by every service.
 *
 * One topic per "source" service (order-events, inventory-events, ...) is a
 * common convention: the producing service owns the topic, and any service
 * that cares subscribes with its own consumer group.
 */
public final class Topics {

    public static final String ORDER_EVENTS = "order-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String PRODUCT_EVENTS = "product-events";

    private Topics() {
    }
}
