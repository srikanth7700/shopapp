package com.shopstream.notification.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.findForUser(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("count", notificationService.unreadCount(userId));
    }

    /** PATCH = partial update: only the "read" flag changes. */
    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return notificationService.markRead(id, userId);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("updated", notificationService.markAllRead(userId));
    }
}
