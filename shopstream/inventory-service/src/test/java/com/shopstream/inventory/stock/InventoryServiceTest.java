package com.shopstream.inventory.stock;

import com.shopstream.common.events.DomainEvent;
import com.shopstream.common.events.InventoryRejectedEvent;
import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.common.events.OrderItemPayload;
import com.shopstream.common.events.Topics;
import com.shopstream.common.kafka.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository itemRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    private final InventoryItem headphones = new InventoryItem(1L, "SS-ELEC-001", 5);
    private final InventoryItem keyboard = new InventoryItem(4L, "SS-ELEC-004", 2);

    private OrderCreatedEvent order(int headphoneQty, int keyboardQty) {
        return OrderCreatedEvent.of(100L, 7L, "jane@example.com", List.of(
                new OrderItemPayload(1L, "Headphones", headphoneQty, new BigDecimal("199.99")),
                new OrderItemPayload(4L, "Keyboard", keyboardQty, new BigDecimal("149.50"))),
                new BigDecimal("500.00"));
    }

    @Test
    void reservesAllItemsWhenEnoughStock() {
        when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of(headphones, keyboard));

        inventoryService.reserve(order(2, 1));

        assertThat(headphones.getAvailableQuantity()).isEqualTo(3);
        assertThat(headphones.getReservedQuantity()).isEqualTo(2);
        assertThat(keyboard.getAvailableQuantity()).isEqualTo(1);
        ArgumentCaptor<DomainEvent> published = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eq(Topics.INVENTORY_EVENTS), eq(100L), published.capture());
        assertThat(published.getValue()).isInstanceOf(InventoryReservedEvent.class);
    }

    @Test
    void rejectsWholeOrderWhenOneItemIsShort() {
        when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of(headphones, keyboard));

        inventoryService.reserve(order(2, 3));

        // All or nothing: the headphones must not be reserved either.
        assertThat(headphones.getAvailableQuantity()).isEqualTo(5);
        assertThat(keyboard.getAvailableQuantity()).isEqualTo(2);
        ArgumentCaptor<DomainEvent> published = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eq(Topics.INVENTORY_EVENTS), eq(100L), published.capture());
        assertThat(published.getValue()).isInstanceOf(InventoryRejectedEvent.class);
        assertThat(((InventoryRejectedEvent) published.getValue()).reason()).contains("Keyboard");
    }

    @Test
    void duplicateEventIsIgnored() {
        when(reservationRepository.findByOrderId(100L))
                .thenReturn(Optional.of(StockReservation.reserved(100L, List.of(new ReservationLine(1L, 2)))));

        inventoryService.reserve(order(2, 1));

        verifyNoInteractions(itemRepository, eventPublisher);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancellationReleasesReservedStock() {
        headphones.reserve(2);
        StockReservation reservation = StockReservation.reserved(100L, List.of(new ReservationLine(1L, 2)));
        when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.of(reservation));
        when(itemRepository.findAllForUpdate(List.of(1L))).thenReturn(List.of(headphones));

        inventoryService.release(OrderCancelledEvent.of(100L, 7L, "jane@example.com", "Payment declined"));

        assertThat(headphones.getAvailableQuantity()).isEqualTo(5);
        assertThat(headphones.getReservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }
}
