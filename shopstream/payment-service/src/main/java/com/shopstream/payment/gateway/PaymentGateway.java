package com.shopstream.payment.gateway;

import java.math.BigDecimal;

/**
 * The payment provider behind an interface (Strategy pattern).
 * Today it is FakePaymentGateway; swapping in Stripe or Adyen means writing
 * one new class, and PaymentService does not change.
 */
public interface PaymentGateway {

    PaymentResult charge(Long orderId, Long userId, BigDecimal amount);

    record PaymentResult(boolean approved, String transactionRef, String declineReason) {

        public static PaymentResult approved(String transactionRef) {
            return new PaymentResult(true, transactionRef, null);
        }

        public static PaymentResult declined(String reason) {
            return new PaymentResult(false, null, reason);
        }
    }
}
