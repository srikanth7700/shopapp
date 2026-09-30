package com.shopstream.order.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // "order" is a reserved word in SQL
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * cascade = ALL: saving the order saves its items.
     * orphanRemoval: removing an item from the list deletes its row.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Optimistic locking. Hibernate adds "WHERE version = ?" to every UPDATE.
     * If two saga events update the same order at the same moment, the second
     * update fails with an OptimisticLockException; the Kafka error handler then
     * retries it against the fresh row.
     */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(Long userId, String userEmail) {
        this.userId = userId;
        this.userEmail = userEmail;
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

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        totalAmount = totalAmount.add(item.getLineTotal());
    }

    /*
     * State transitions return false when the change does not apply. That makes
     * the saga safe against duplicate and out-of-order events: for example a
     * late InventoryReservedEvent arriving after the order was already
     * CONFIRMED is simply ignored.
     */

    public boolean markInventoryReserved() {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        status = OrderStatus.INVENTORY_RESERVED;
        return true;
    }

    public boolean confirm() {
        if (status.isFinal()) {
            return false;
        }
        status = OrderStatus.CONFIRMED;
        statusReason = null;
        return true;
    }

    public boolean cancel(String reason) {
        if (status.isFinal()) {
            return false;
        }
        status = OrderStatus.CANCELLED;
        statusReason = reason;
        return true;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
