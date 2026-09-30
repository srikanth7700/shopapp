package com.shopstream.order.order;

import com.shopstream.common.events.DomainEvent;
import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.order.client.ProductClient;
import com.shopstream.order.client.ProductSnapshot;
import com.shopstream.order.outbox.OutboxService;
import com.shopstream.order.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository historyRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private OutboxService outboxService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // A TransactionTemplate over a mock transaction manager just runs the callback.
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        orderService = new OrderService(orderRepository, historyRepository, productClient, outboxService, transactionTemplate);
    }

    @Test
    void placeOrderUsesCatalogPricesAndWritesOutboxEvent() {
        when(productClient.getProducts(anyCollection())).thenReturn(List.of(
                new ProductSnapshot(1L, "Headphones", new BigDecimal("199.99"), true),
                new ProductSnapshot(5L, "French Press", new BigDecimal("34.95"), true)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 10L);
            return order;
        });

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.Line(1L, 1),
                new CreateOrderRequest.Line(5L, 2),
                new CreateOrderRequest.Line(1L, 1))); // duplicate line gets merged

        OrderResponse response = orderService.placeOrder(7L, "jane@example.com", request);

        // 2 x 199.99 + 2 x 34.95 = 469.88
        assertThat(response.totalAmount()).isEqualByComparingTo("469.88");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.items()).hasSize(2);

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outboxService).enqueue(eq(Topics.ORDER_EVENTS), eq(10L), event.capture());
        OrderCreatedEvent created = (OrderCreatedEvent) event.getValue();
        assertThat(created.orderId()).isEqualTo(10L);
        assertThat(created.totalAmount()).isEqualByComparingTo("469.88");
    }

    @Test
    void placeOrderRejectsInactiveProduct() {
        when(productClient.getProducts(anyCollection())).thenReturn(List.of(
                new ProductSnapshot(1L, "Headphones", new BigDecimal("199.99"), false)));

        CreateOrderRequest request = new CreateOrderRequest(List.of(new CreateOrderRequest.Line(1L, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(7L, "jane@example.com", request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not available");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void paymentFailureCancelsOrderAndEmitsCompensationEvent() {
        Order order = new Order(7L, "jane@example.com");
        ReflectionTestUtils.setField(order, "id", 10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.cancel(10L, "Payment declined");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outboxService).enqueue(eq(Topics.ORDER_EVENTS), eq(10L), event.capture());
        assertThat(event.getValue()).isInstanceOf(OrderCancelledEvent.class);
    }

    @Test
    void lateEventsDoNotChangeAFinalOrder() {
        Order order = new Order(7L, "jane@example.com");
        ReflectionTestUtils.setField(order, "id", 10L);
        order.confirm();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.cancel(10L, "Duplicate or late event");
        orderService.markInventoryReserved(10L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(outboxService, never()).enqueue(any(), any(), any());
    }
}
