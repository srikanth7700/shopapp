package com.shopstream.inventory.stock;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One row per order. The UNIQUE order_id column is what makes the Kafka
 * consumer idempotent: if the same OrderCreatedEvent arrives twice, the second
 * time we find this row and do nothing.
 */
@Entity
@Table(name = "stock_reservations")
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(length = 1000)
    private String reason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stock_reservation_lines", joinColumns = @JoinColumn(name = "reservation_id"))
    private List<ReservationLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockReservation() {
    }

    private StockReservation(Long orderId, ReservationStatus status, String reason, List<ReservationLine> lines) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.lines = new ArrayList<>(lines);
    }

    public static StockReservation reserved(Long orderId, List<ReservationLine> lines) {
        return new StockReservation(orderId, ReservationStatus.RESERVED, null, lines);
    }

    public static StockReservation rejected(Long orderId, String reason) {
        return new StockReservation(orderId, ReservationStatus.REJECTED, reason, List.of());
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markCommitted() {
        this.status = ReservationStatus.COMMITTED;
    }

    public void markReleased(String reason) {
        this.status = ReservationStatus.RELEASED;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public List<ReservationLine> getLines() {
        return lines;
    }
}
