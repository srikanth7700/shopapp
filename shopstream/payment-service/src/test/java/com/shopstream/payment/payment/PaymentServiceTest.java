package com.shopstream.payment.payment;

import com.shopstream.common.events.DomainEvent;
import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.PaymentCompletedEvent;
import com.shopstream.common.events.PaymentFailedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.common.kafka.EventPublisher;
import com.shopstream.payment.gateway.FakePaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventPublisher eventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Real fake gateway with a 5000 limit and no artificial delay.
        paymentService = new PaymentService(paymentRepository, new FakePaymentGateway(new BigDecimal("5000"), 0),
                eventPublisher);
    }

    @Test
    void approvesAmountWithinLimit() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(InventoryReservedEvent.of(1L, 7L, new BigDecimal("199.99")));

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eq(Topics.PAYMENT_EVENTS), eq(1L), event.capture());
        assertThat(event.getValue()).isInstanceOf(PaymentCompletedEvent.class);
    }

    @Test
    void declinesAmountAboveLimit() {
        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(InventoryReservedEvent.of(2L, 7L, new BigDecimal("5499.00")));

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eq(Topics.PAYMENT_EVENTS), eq(2L), event.capture());
        assertThat(event.getValue()).isInstanceOf(PaymentFailedEvent.class);
        assertThat(((PaymentFailedEvent) event.getValue()).reason()).contains("5,499.00");
    }

    @Test
    void duplicateEventDoesNotChargeTwice() {
        Payment earlier = Payment.completed(3L, 7L, new BigDecimal("10.00"), "TXN-ABC");
        when(paymentRepository.findByOrderId(3L)).thenReturn(Optional.of(earlier));

        paymentService.processPayment(InventoryReservedEvent.of(3L, 7L, new BigDecimal("10.00")));

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher).publish(eq(Topics.PAYMENT_EVENTS), eq(3L), any(PaymentCompletedEvent.class));
    }
}
