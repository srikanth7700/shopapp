package com.shopstream.order.order;

import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderConfirmedEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.common.events.OrderItemPayload;
import com.shopstream.common.events.Topics;
import com.shopstream.order.client.ProductClient;
import com.shopstream.order.client.ProductSnapshot;
import com.shopstream.order.outbox.OutboxService;
import com.shopstream.order.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductClient productClient;
    private final OutboxService outboxService;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepository,
                        OrderStatusHistoryRepository historyRepository,
                        ProductClient productClient,
                        OutboxService outboxService,
                        TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.productClient = productClient;
        this.outboxService = outboxService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Saga step 1: create the order in PENDING state and emit OrderCreatedEvent.
     *
     * Not annotated with @Transactional on purpose: the HTTP call to
     * product-service happens first, OUTSIDE any transaction, so we do not hold
     * a database connection while waiting on the network. Only the DB writes run
     * inside the TransactionTemplate block.
     */
    public OrderResponse placeOrder(Long userId, String userEmail, CreateOrderRequest request) {
        // Merge duplicate lines: two lines for product 5 become one line with the summed quantity.
        Map<Long, Integer> quantities = request.items().stream().collect(Collectors.toMap(
                CreateOrderRequest.Line::productId, CreateOrderRequest.Line::quantity, Integer::sum, LinkedHashMap::new));

        Map<Long, ProductSnapshot> products = productClient.getProducts(quantities.keySet()).stream()
                .collect(Collectors.toMap(ProductSnapshot::id, Function.identity()));

        Order order = new Order(userId, userEmail);
        quantities.forEach((productId, quantity) -> {
            ProductSnapshot product = products.get(productId);
            if (product == null || !product.active()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Product " + productId + " is not available");
            }
            order.addItem(new OrderItem(product.id(), product.name(), product.price(), quantity));
        });

        Order saved = transactionTemplate.execute(status -> {
            Order persisted = orderRepository.save(order);
            historyRepository.save(new OrderStatusHistory(persisted.getId(), OrderStatus.PENDING, "Order placed"));
            List<OrderItemPayload> items = persisted.getItems().stream()
                    .map(i -> new OrderItemPayload(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                    .toList();
            outboxService.enqueue(Topics.ORDER_EVENTS, persisted.getId(), OrderCreatedEvent.of(
                    persisted.getId(), userId, userEmail, items, persisted.getTotalAmount()));
            return persisted;
        });
        log.info("Order {} placed by user {} for {}", saved.getId(), userId, saved.getTotalAmount());
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findOne(Long orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findWithItemsById(orderId)
                .filter(o -> isAdmin || o.getUserId().equals(userId))
                // 404 rather than 403, so users cannot probe which order ids exist.
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order " + orderId + " not found"));
        return OrderResponse.from(order, historyRepository.findByOrderIdOrderByChangedAtAscIdAsc(orderId));
    }

    // ----- Saga reactions (called by OrderSagaListener) -----

    @Transactional
    public void markInventoryReserved(Long orderId) {
        withOrder(orderId, order -> {
            if (order.markInventoryReserved()) {
                historyRepository.save(new OrderStatusHistory(orderId, OrderStatus.INVENTORY_RESERVED,
                        "Stock reserved, waiting for payment"));
            }
        });
    }

    @Transactional
    public void confirm(Long orderId, String note) {
        withOrder(orderId, order -> {
            if (order.confirm()) {
                historyRepository.save(new OrderStatusHistory(orderId, OrderStatus.CONFIRMED, note));
                outboxService.enqueue(Topics.ORDER_EVENTS, orderId, OrderConfirmedEvent.of(
                        orderId, order.getUserId(), order.getUserEmail(), order.getTotalAmount()));
            } else {
                log.info("Ignoring confirm for order {} in status {}", orderId, order.getStatus());
            }
        });
    }

    @Transactional
    public void cancel(Long orderId, String reason) {
        withOrder(orderId, order -> {
            if (order.cancel(reason)) {
                historyRepository.save(new OrderStatusHistory(orderId, OrderStatus.CANCELLED, reason));
                // inventory-service listens for this and releases any reserved stock (compensation).
                outboxService.enqueue(Topics.ORDER_EVENTS, orderId, OrderCancelledEvent.of(
                        orderId, order.getUserId(), order.getUserEmail(), reason));
            } else {
                log.info("Ignoring cancel for order {} in status {}", orderId, order.getStatus());
            }
        });
    }

    private void withOrder(Long orderId, Consumer<Order> action) {
        orderRepository.findById(orderId).ifPresentOrElse(action,
                () -> log.warn("Received saga event for unknown order {}", orderId));
    }
}
