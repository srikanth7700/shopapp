package com.shopstream.order.messaging;

import com.shopstream.common.events.InventoryRejectedEvent;
import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.PaymentCompletedEvent;
import com.shopstream.common.events.PaymentFailedEvent;
import com.shopstream.order.order.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderSagaListener listener;

    @Test
    void inventoryReservedMovesOrderToInventoryReserved() {
        listener.onInventoryReserved(InventoryReservedEvent.of(17L, 23L, new BigDecimal("49.95")));

        verify(orderService).markInventoryReserved(17L);
    }

    @Test
    void inventoryRejectedCancelsOrderWithRejectionReason() {
        listener.onInventoryRejected(InventoryRejectedEvent.of(17L, 23L, "Insufficient stock"));

        verify(orderService).cancel(17L, "Insufficient stock");
    }

    @Test
    void paymentCompletedConfirmsOrderWithTransactionReference() {
        listener.onPaymentCompleted(PaymentCompletedEvent.of(
                17L, 23L, 31L, "TXN-17", new BigDecimal("49.95")));

        verify(orderService).confirm(17L, "Payment approved, transaction TXN-17");
    }

    @Test
    void paymentFailedCancelsOrderWithFailureReason() {
        listener.onPaymentFailed(PaymentFailedEvent.of(
                17L, 23L, new BigDecimal("49.95"), "Payment declined"));

        verify(orderService).cancel(17L, "Payment declined");
    }

    @Test
    void unknownEventIsIgnored() {
        listener.ignore(new Object());

        verifyNoInteractions(orderService);
    }
}
