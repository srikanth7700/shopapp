package com.shopstream.inventory.stock;

import java.time.Instant;

public record StockResponse(Long productId, String sku, int availableQuantity, int reservedQuantity, Instant updatedAt) {

    public static StockResponse from(InventoryItem item) {
        return new StockResponse(item.getProductId(), item.getSku(), item.getAvailableQuantity(),
                item.getReservedQuantity(), item.getUpdatedAt());
    }
}
