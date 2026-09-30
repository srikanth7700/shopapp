package com.shopstream.inventory.messaging;

import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderConfirmedEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.common.events.ProductCreatedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.inventory.stock.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * A class-level @KafkaListener with one @KafkaHandler per event type.
 * Spring deserializes the JSON into the right record (using the type header)
 * and calls the method whose parameter matches.
 *
 * Listeners stay thin: they translate a message into a service call.
 * All logic and transactions live in InventoryService.
 */
@Component
@KafkaListener(topics = {Topics.ORDER_EVENTS, Topics.PRODUCT_EVENTS})
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryService inventoryService;

    public InventoryEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaHandler
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order {}", event.orderId());
        inventoryService.reserve(event);
    }

    @KafkaHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order {}", event.orderId());
        inventoryService.release(event);
    }

    @KafkaHandler
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received OrderConfirmedEvent for order {}", event.orderId());
        inventoryService.commit(event);
    }

    @KafkaHandler
    public void onProductCreated(ProductCreatedEvent event) {
        inventoryService.createStockItem(event);
    }

    /** Events this service does not care about. */
    @KafkaHandler(isDefault = true)
    public void ignore(Object event) {
        log.debug("Ignoring {}", event.getClass().getSimpleName());
    }
}
