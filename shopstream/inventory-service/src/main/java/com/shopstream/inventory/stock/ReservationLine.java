package com.shopstream.inventory.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A value object stored in its own table but with no identity of its own (see StockReservation.lines). */
@Embeddable
public class ReservationLine {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    protected ReservationLine() {
    }

    public ReservationLine(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
