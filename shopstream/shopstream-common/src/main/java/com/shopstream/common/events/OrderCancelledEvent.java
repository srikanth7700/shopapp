package com.shopstream.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by order-service when the saga fails (no stock or payment declined).
 * inventory-service reacts by releasing any stock it reserved: this is the
 * "compensating action" of the saga.
 */
public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String userEmail,
        String reason) implements DomainEvent {

    public static OrderCancelledEvent of(Long orderId, Long userId, String userEmail, String reason) {
        return new OrderCancelledEvent(UUID.randomUUID(), Instant.now(), orderId, userId, userEmail, reason);
    }
}
