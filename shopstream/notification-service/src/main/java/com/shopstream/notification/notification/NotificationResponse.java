package com.shopstream.notification.notification;

import java.time.Instant;

public record NotificationResponse(Long id, Long orderId, String type, String title, String message,
                                   boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getOrderId(), n.getType().name(), n.getTitle(), n.getMessage(),
                n.isRead(), n.getCreatedAt());
    }
}
