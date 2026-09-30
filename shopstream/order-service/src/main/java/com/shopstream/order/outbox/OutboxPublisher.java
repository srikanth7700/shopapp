package com.shopstream.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox table and sends pending events to Kafka.
 *
 * Production alternative: Change Data Capture (Debezium) reads the database's
 * write-ahead log and publishes outbox rows without polling.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxRepository.lockNextBatch(BATCH_SIZE);
        for (OutboxEvent outboxEvent : batch) {
            try {
                Object event = objectMapper.readValue(outboxEvent.getPayload(), Class.forName(outboxEvent.getEventType()));
                kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getMessageKey(), event).get(10, TimeUnit.SECONDS);
                outboxEvent.markPublished();
                log.info("Outbox -> Kafka: {} (key={}) to {}", event.getClass().getSimpleName(),
                        outboxEvent.getMessageKey(), outboxEvent.getTopic());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                // Stop at the first failure so events keep their order. The rows
                // already marked as published in this batch are still committed.
                log.warn("Could not publish outbox event {}; will retry: {}", outboxEvent.getId(), ex.getMessage());
                return;
            }
        }
    }

    /** Housekeeping: published rows are only needed for a while (debugging, audits). */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldPublishedEvents() {
        int deleted = outboxRepository.deletePublishedBefore(Instant.now().minus(Duration.ofDays(7)));
        log.info("Deleted {} old outbox rows", deleted);
    }
}
