package com.shopstream.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by payment-service when the charge went through. */
public record PaymentCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        Long paymentId,
        String transactionRef,
        BigDecimal amount) implements DomainEvent {

    public static PaymentCompletedEvent of(Long orderId, Long userId, Long paymentId,
                                           String transactionRef, BigDecimal amount) {
        return new PaymentCompletedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, paymentId, transactionRef, amount);
    }
}
