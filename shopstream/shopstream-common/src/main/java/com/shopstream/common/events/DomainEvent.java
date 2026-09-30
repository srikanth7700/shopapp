package com.shopstream.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Every event carries a unique id and a timestamp.
 *
 * The eventId lets consumers detect duplicates. Kafka gives "at-least-once"
 * delivery, so the same message can arrive twice (for example after a consumer
 * restart). Consumers must be idempotent: processing an event twice has the
 * same effect as processing it once.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}
