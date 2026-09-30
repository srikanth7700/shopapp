package com.shopstream.payment.payment;

import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.PaymentCompletedEvent;
import com.shopstream.common.events.PaymentFailedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.common.kafka.EventPublisher;
import com.shopstream.payment.gateway.PaymentGateway;
import com.shopstream.payment.gateway.PaymentGateway.PaymentResult;
import com.shopstream.payment.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
                          EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Saga step 3: stock is reserved, now charge the customer.
     */
    @Transactional
    public void processPayment(InventoryReservedEvent event) {
        Optional<Payment> existing = paymentRepository.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            // Never charge twice. Re-publishing the earlier result is safe because
            // order-service ignores results for orders that are already final.
            log.info("Order {} was already charged, re-publishing the result", event.orderId());
            publishResult(existing.get());
            return;
        }

        PaymentResult result = paymentGateway.charge(event.orderId(), event.userId(), event.totalAmount());
        Payment payment = result.approved()
                ? Payment.completed(event.orderId(), event.userId(), event.totalAmount(), result.transactionRef())
                : Payment.failed(event.orderId(), event.userId(), event.totalAmount(), result.declineReason());
        publishResult(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findForUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findForOrder(Long orderId, Long userId, boolean isAdmin) {
        return paymentRepository.findByOrderId(orderId)
                .filter(p -> isAdmin || p.getUserId().equals(userId))
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No payment for order " + orderId));
    }

    private void publishResult(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            eventPublisher.publish(Topics.PAYMENT_EVENTS, payment.getOrderId(), PaymentCompletedEvent.of(
                    payment.getOrderId(), payment.getUserId(), payment.getId(), payment.getTransactionRef(),
                    payment.getAmount()));
        } else {
            eventPublisher.publish(Topics.PAYMENT_EVENTS, payment.getOrderId(), PaymentFailedEvent.of(
                    payment.getOrderId(), payment.getUserId(), payment.getAmount(), payment.getFailureReason()));
        }
    }
}
