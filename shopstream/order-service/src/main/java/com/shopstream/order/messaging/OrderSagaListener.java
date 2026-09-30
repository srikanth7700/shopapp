package com.shopstream.order.messaging;

import com.shopstream.common.events.InventoryRejectedEvent;
import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.PaymentCompletedEvent;
import com.shopstream.common.events.PaymentFailedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.order.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * order-service is the one place that knows the final state of every order.
 * It listens to the results of the other saga steps and moves the order along.
 *
 * This is a "choreography" saga: no central coordinator tells services what to
 * do; each service reacts to events. The alternative, "orchestration", has one
 * component send explicit commands to each step.
 */
@Component
@KafkaListener(topics = {Topics.INVENTORY_EVENTS, Topics.PAYMENT_EVENTS})
public class OrderSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaListener.class);

    private final OrderService orderService;

    public OrderSagaListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaHandler
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Order {}: inventory reserved", event.orderId());
        orderService.markInventoryReserved(event.orderId());
    }

    @KafkaHandler
    public void onInventoryRejected(InventoryRejectedEvent event) {
        log.info("Order {}: inventory rejected ({})", event.orderId(), event.reason());
        orderService.cancel(event.orderId(), event.reason());
    }

    @KafkaHandler
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Order {}: payment completed ({})", event.orderId(), event.transactionRef());
        orderService.confirm(event.orderId(), "Payment approved, transaction " + event.transactionRef());
    }

    @KafkaHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Order {}: payment failed ({})", event.orderId(), event.reason());
        orderService.cancel(event.orderId(), event.reason());
    }

    @KafkaHandler(isDefault = true)
    public void ignore(Object event) {
        log.debug("Ignoring {}", event.getClass().getSimpleName());
    }
}
