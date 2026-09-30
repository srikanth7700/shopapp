package com.shopstream.product.product;

import com.shopstream.common.events.ProductCreatedEvent;
import com.shopstream.common.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the in-process Spring event to Kafka once the database transaction has committed.
 *
 * Compare with order-service, which uses a transactional outbox table. This
 * approach is simpler but weaker: if the app crashes right after the commit
 * and before the send, the event is lost. That is acceptable here (an admin can
 * restock manually) but not for orders, where a lost event means a stuck order.
 */
@Component
public class ProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        kafkaTemplate.send(Topics.PRODUCT_EVENTS, String.valueOf(event.productId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductCreatedEvent for product {}", event.productId(), ex);
                    } else {
                        log.info("Published ProductCreatedEvent for product {}", event.productId());
                    }
                });
    }
}
