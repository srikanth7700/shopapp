package com.shopstream.payment.gateway;

import com.shopstream.payment.gateway.PaymentGateway.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * A pretend payment provider:
 * - approves any amount up to app.payment.max-amount (default 5000)
 * - declines anything above it
 * - waits a moment to feel like a real network call
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

    private final BigDecimal maxAmount;
    private final long simulatedDelayMs;

    public FakePaymentGateway(@Value("${app.payment.max-amount:5000}") BigDecimal maxAmount,
                              @Value("${app.payment.simulated-delay-ms:1500}") long simulatedDelayMs) {
        this.maxAmount = maxAmount;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    @Override
    public PaymentResult charge(Long orderId, Long userId, BigDecimal amount) {
        simulateNetworkDelay();
        if (amount.compareTo(maxAmount) > 0) {
            log.info("Declining order {}: {} is above the {} limit", orderId, amount, maxAmount);
            return PaymentResult.declined(String.format(Locale.US,
                    "Payment declined: $%,.2f is above the $%,.2f limit of the demo payment provider",
                    amount, maxAmount));
        }
        String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        log.info("Approved order {} for {} ({})", orderId, amount, transactionRef);
        return PaymentResult.approved(transactionRef);
    }

    private void simulateNetworkDelay() {
        if (simulatedDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
