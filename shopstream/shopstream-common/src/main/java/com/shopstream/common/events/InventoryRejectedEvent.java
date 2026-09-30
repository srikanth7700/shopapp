package com.shopstream.common.events;

import java.time.Instant;
import java.util.UUID;

/** Published by inventory-service when at least one item is out of stock. */
public record InventoryRejectedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String reason) implements DomainEvent {

    public static InventoryRejectedEvent of(Long orderId, Long userId, String reason) {
        return new InventoryRejectedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, reason);
    }
}
