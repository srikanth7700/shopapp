package com.shopstream.payment.messaging;

import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.payment.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = Topics.INVENTORY_EVENTS)
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentService paymentService;

    public PaymentEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaHandler
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Charging order {} ({})", event.orderId(), event.totalAmount());
        paymentService.processPayment(event);
    }

    /** InventoryRejectedEvent and anything else: nothing to charge. */
    @KafkaHandler(isDefault = true)
    public void ignore(Object event) {
        log.debug("Ignoring {}", event.getClass().getSimpleName());
    }
}
