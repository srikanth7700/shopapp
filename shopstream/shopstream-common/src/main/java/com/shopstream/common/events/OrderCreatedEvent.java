package com.shopstream.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published by order-service when a customer places an order. Starts the saga. */
public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String userEmail,
        List<OrderItemPayload> items,
        BigDecimal totalAmount) implements DomainEvent {

    public static OrderCreatedEvent of(Long orderId, Long userId, String userEmail,
                                       List<OrderItemPayload> items, BigDecimal totalAmount) {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, userEmail, items, totalAmount);
    }
}
