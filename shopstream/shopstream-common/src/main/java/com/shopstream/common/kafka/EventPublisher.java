package com.shopstream.common.kafka;

import com.shopstream.common.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends an event and waits for Kafka to acknowledge it.
 *
 * Why wait? KafkaTemplate.send() is asynchronous. If we fire-and-forget inside
 * a database transaction and the send later fails, the database says "done"
 * but nobody downstream ever hears about it. By waiting, a failed send throws,
 * the transaction rolls back, and the Kafka listener that triggered this work
 * retries the whole thing.
 *
 * The remaining gap: the send succeeds but the DB commit fails. Then the event
 * is retried and sent twice, which is why every consumer is idempotent.
 * order-service closes this gap completely with the transactional outbox pattern.
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, Object key, DomainEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(key), event).get(10, TimeUnit.SECONDS);
            log.info("Published {} to {} (key={})", event.getClass().getSimpleName(), topic, key);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing " + event.getClass().getSimpleName(), ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Could not publish " + event.getClass().getSimpleName() + " to " + topic, ex);
        }
    }
}
