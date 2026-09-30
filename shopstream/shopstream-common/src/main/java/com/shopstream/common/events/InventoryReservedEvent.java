package com.shopstream.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by inventory-service when every item in the order was reserved. */
public record InventoryReservedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        BigDecimal totalAmount) implements DomainEvent {

    public static InventoryReservedEvent of(Long orderId, Long userId, BigDecimal totalAmount) {
        return new InventoryReservedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, totalAmount);
    }
}
