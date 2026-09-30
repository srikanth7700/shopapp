package com.shopstream.order.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstream.common.events.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Transactional Outbox pattern.
 *
 * The problem ("dual write"): placing an order must (1) insert the order row
 * and (2) send OrderCreatedEvent to Kafka. A database transaction cannot
 * include Kafka, so one of the two can succeed while the other fails:
 *   - DB commit ok, Kafka send fails  -> order stuck in PENDING forever
 *   - Kafka send ok, DB rolls back    -> other services act on an order that does not exist
 *
 * The fix: write the event into an "outbox" table in the SAME transaction as
 * the order. Either both rows are saved or neither is. A separate poller
 * (OutboxPublisher) then sends pending rows to Kafka and marks them published.
 * If sending fails, the row stays pending and is retried: at-least-once delivery.
 */
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /** MANDATORY: throws if called outside a transaction, because that would defeat the whole point. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, Object key, DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(topic, String.valueOf(key), event.getClass().getName(), payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize " + event.getClass().getSimpleName(), ex);
        }
    }
}
