package com.shopstream.inventory.stock;

public enum ReservationStatus {
    /** Stock is held while payment runs. */
    RESERVED,
    /** Order confirmed, stock is sold. */
    COMMITTED,
    /** Order cancelled after reserving, stock went back to available. */
    RELEASED,
    /** Not enough stock, nothing was reserved. */
    REJECTED
}
