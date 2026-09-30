package com.shopstream.order.order;

/**
 * The saga's state machine, as seen by the order:
 *
 *   PENDING --(stock reserved)--> INVENTORY_RESERVED --(payment ok)--> CONFIRMED
 *      |                                  |
 *      +--(out of stock)--> CANCELLED <---+--(payment declined)
 */
public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    CONFIRMED,
    CANCELLED;

    public boolean isFinal() {
        return this == CONFIRMED || this == CANCELLED;
    }
}
