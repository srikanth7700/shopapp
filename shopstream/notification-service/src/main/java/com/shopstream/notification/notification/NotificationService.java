package com.shopstream.notification.notification;

import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderConfirmedEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.notification.email.EmailSender;
import com.shopstream.notification.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;

    public NotificationService(NotificationRepository notificationRepository, EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        createNotification(event.eventId(), event.userId(), event.userEmail(), event.orderId(), NotificationType.ORDER_PLACED,
                "Order #" + event.orderId() + " received",
                "We received your order for " + money(event.totalAmount())
                        + ". We'll confirm it as soon as stock and payment are checked.");
    }

    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        createNotification(event.eventId(), event.userId(), event.userEmail(), event.orderId(), NotificationType.ORDER_CONFIRMED,
                "Order #" + event.orderId() + " confirmed",
                "Payment of " + money(event.totalAmount()) + " went through. Your order is confirmed.");
    }

    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        createNotification(event.eventId(), event.userId(), event.userEmail(), event.orderId(), NotificationType.ORDER_CANCELLED,
                "Order #" + event.orderId() + " cancelled",
                "Your order was cancelled and you were not charged. Reason: " + event.reason());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findForUser(Long userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    private void createNotification(UUID eventId, Long userId, String email, Long orderId, NotificationType type,
                                    String title, String message) {
        if (notificationRepository.existsBySourceEventId(eventId)) {
            return; // already handled this exact event
        }
        notificationRepository.save(new Notification(eventId, userId, orderId, type, title, message));
        emailSender.send(email, title, message);
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }
}
