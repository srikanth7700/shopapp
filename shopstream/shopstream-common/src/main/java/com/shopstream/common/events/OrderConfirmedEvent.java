package com.shopstream.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by order-service when payment succeeded. The saga ended well. */
public record OrderConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal totalAmount) implements DomainEvent {

    public static OrderConfirmedEvent of(Long orderId, Long userId, String userEmail, BigDecimal totalAmount) {
        return new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, userEmail, totalAmount);
    }
}
