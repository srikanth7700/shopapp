package com.shopstream.inventory.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Stock for one product.
 *
 * available = can be sold right now
 * reserved  = held for orders whose payment is still in progress
 *
 * Holding stock during payment prevents two customers buying the last unit.
 * The business rules live on the entity itself (reserve/release/commit), not in
 * a service full of setters: this keeps the invariants in one place.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    /** Same id as in product-service. Not generated here: product-service owns product ids. */
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(Long productId, String sku, int availableQuantity) {
        this.productId = productId;
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Not enough stock for product " + productId);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    /** Compensation: the order failed, put the units back on the shelf. */
    public void release(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    /** The order is confirmed: the reserved units are now sold. */
    public void commit(int quantity) {
        reservedQuantity -= quantity;
    }

    public void restock(int quantity) {
        availableQuantity += quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
