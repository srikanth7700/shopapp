package com.shopstream.common.events;

import java.time.Instant;
import java.util.UUID;

/** Published by product-service when an admin adds a product, so inventory can start tracking it. */
public record ProductCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long productId,
        String sku,
        String name,
        int initialStock) implements DomainEvent {

    public static ProductCreatedEvent of(Long productId, String sku, String name, int initialStock) {
        return new ProductCreatedEvent(UUID.randomUUID(), Instant.now(), productId, sku, name, initialStock);
    }
}
