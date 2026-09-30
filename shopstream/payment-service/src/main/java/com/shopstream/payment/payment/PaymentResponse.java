package com.shopstream.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long orderId, BigDecimal amount, String status,
                              String transactionRef, String failureReason, Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus().name(),
                p.getTransactionRef(), p.getFailureReason(), p.getCreatedAt());
    }
}
