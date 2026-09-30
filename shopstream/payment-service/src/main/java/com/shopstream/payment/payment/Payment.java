package com.shopstream.payment.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    /** UNIQUE: an order can be charged at most once, even if the event is delivered twice. */
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_ref", length = 40)
    private String transactionRef;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;



    //djjdjfhdhhgjhrj
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    private Payment(Long orderId, Long userId, BigDecimal amount, PaymentStatus status,
                    String transactionRef, String failureReason) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.transactionRef = transactionRef;
        this.failureReason = failureReason;
    }

    public static Payment completed(Long orderId, Long userId, BigDecimal amount, String transactionRef) {
        return new Payment(orderId, userId, amount, PaymentStatus.COMPLETED, transactionRef, null);
    }

    public static Payment failed(Long orderId, Long userId, BigDecimal amount, String reason) {
        return new Payment(orderId, userId, amount, PaymentStatus.FAILED, null, reason);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
