package com.shopstream.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        String statusReason,
        BigDecimal totalAmount,
        List<Item> items,
        List<StatusChange> history,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record StatusChange(String status, String note, Instant changedAt) {
    }

    public static OrderResponse from(Order order) {
        return from(order, List.of());
    }

    public static OrderResponse from(Order order, List<OrderStatusHistory> history) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
        List<StatusChange> changes = history.stream()
                .map(h -> new StatusChange(h.getStatus().name(), h.getNote(), h.getChangedAt()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getStatusReason(),
                order.getTotalAmount(), items, changes, order.getCreatedAt(), order.getUpdatedAt());
    }
}
