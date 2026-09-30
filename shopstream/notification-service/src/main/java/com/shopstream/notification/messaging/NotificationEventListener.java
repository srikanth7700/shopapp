package com.shopstream.notification.messaging;

import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderConfirmedEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.notification.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * notification-service only listens; it never publishes. It is a good example
 * of why events decouple services: order-service does not know this service
 * exists, and you could add an analytics-service tomorrow the same way.
 */
@Component
@KafkaListener(topics = Topics.ORDER_EVENTS)
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaHandler
    public void onOrderCreated(OrderCreatedEvent event) {
        notificationService.onOrderCreated(event);
    }

    @KafkaHandler
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        notificationService.onOrderConfirmed(event);
    }

    @KafkaHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        notificationService.onOrderCancelled(event);
    }

    @KafkaHandler(isDefault = true)
    public void ignore(Object event) {
        log.debug("Ignoring {}", event.getClass().getSimpleName());
    }
}
