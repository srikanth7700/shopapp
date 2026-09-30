package com.shopstream.notification.notification;

import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.notification.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void cancelledOrderCreatesNotificationAndSendsEmail() {
        OrderCancelledEvent event = OrderCancelledEvent.of(5L, 7L, "jane@example.com", "Out of stock");
        when(notificationRepository.existsBySourceEventId(event.eventId())).thenReturn(false);

        notificationService.onOrderCancelled(event);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(saved.getValue().getMessage()).contains("Out of stock");
        verify(emailSender).send(eq("jane@example.com"), contains("#5"), anyString());
    }

    @Test
    void sameEventTwiceOnlyNotifiesOnce() {
        OrderCancelledEvent event = OrderCancelledEvent.of(5L, 7L, "jane@example.com", "Out of stock");
        when(notificationRepository.existsBySourceEventId(event.eventId())).thenReturn(true);

        notificationService.onOrderCancelled(event);

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(emailSender);
    }
}
