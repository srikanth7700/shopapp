package com.shopstream.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by payment-service when the charge was declined. */
public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String reason) implements DomainEvent {

    public static PaymentFailedEvent of(Long orderId, Long userId, BigDecimal amount, String reason) {
        return new PaymentFailedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, amount, reason);
    }
}
