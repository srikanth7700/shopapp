package com.shopstream.inventory.stock;

import com.shopstream.common.events.InventoryRejectedEvent;
import com.shopstream.common.events.InventoryReservedEvent;
import com.shopstream.common.events.OrderCancelledEvent;
import com.shopstream.common.events.OrderConfirmedEvent;
import com.shopstream.common.events.OrderCreatedEvent;
import com.shopstream.common.events.OrderItemPayload;
import com.shopstream.common.events.ProductCreatedEvent;
import com.shopstream.common.events.Topics;
import com.shopstream.common.kafka.EventPublisher;
import com.shopstream.inventory.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository itemRepository;
    private final StockReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;

    public InventoryService(InventoryItemRepository itemRepository,
                            StockReservationRepository reservationRepository,
                            EventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Saga step 2: try to reserve every item of a new order. All or nothing.
     */
    @Transactional
    public void reserve(OrderCreatedEvent event) {
        if (reservationRepository.findByOrderId(event.orderId()).isPresent()) {
            log.info("Order {} already processed, ignoring duplicate event", event.orderId());
            return;
        }

        // Merge duplicate lines and sort by product id (TreeMap) for a consistent lock order.
        Map<Long, Integer> requested = event.items().stream().collect(Collectors.toMap(
                OrderItemPayload::productId, OrderItemPayload::quantity, Integer::sum, TreeMap::new));
        Map<Long, String> names = event.items().stream().collect(Collectors.toMap(
                OrderItemPayload::productId, OrderItemPayload::productName, (a, b) -> a));

        Map<Long, InventoryItem> stock = itemRepository.findAllForUpdate(requested.keySet()).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        List<String> problems = new ArrayList<>();
        requested.forEach((productId, quantity) -> {
            InventoryItem item = stock.get(productId);
            if (item == null) {
                problems.add(names.get(productId) + " is not stocked");
            } else if (!item.canReserve(quantity)) {
                problems.add(names.get(productId) + ": requested " + quantity + ", only "
                        + item.getAvailableQuantity() + " left");
            }
        });

        if (!problems.isEmpty()) {
            String reason = "Out of stock - " + String.join("; ", problems);
            reservationRepository.save(StockReservation.rejected(event.orderId(), reason));
            eventPublisher.publish(Topics.INVENTORY_EVENTS, event.orderId(),
                    InventoryRejectedEvent.of(event.orderId(), event.userId(), reason));
            return;
        }

        List<ReservationLine> lines = new ArrayList<>();
        requested.forEach((productId, quantity) -> {
            stock.get(productId).reserve(quantity);
            lines.add(new ReservationLine(productId, quantity));
        });
        reservationRepository.save(StockReservation.reserved(event.orderId(), lines));
        eventPublisher.publish(Topics.INVENTORY_EVENTS, event.orderId(),
                InventoryReservedEvent.of(event.orderId(), event.userId(), event.totalAmount()));
    }

    /** Compensating action: the order was cancelled after we reserved stock (e.g. payment declined). */
    @Transactional
    public void release(OrderCancelledEvent event) {
        reservationRepository.findByOrderId(event.orderId())
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    Map<Long, InventoryItem> stock = lockItems(reservation);
                    reservation.getLines().forEach(line ->
                            stock.get(line.getProductId()).release(line.getQuantity()));
                    reservation.markReleased(event.reason());
                    log.info("Released stock for cancelled order {}", event.orderId());
                });
    }

    /** The order is confirmed: reserved units become sold units. */
    @Transactional
    public void commit(OrderConfirmedEvent event) {
        reservationRepository.findByOrderId(event.orderId())
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    Map<Long, InventoryItem> stock = lockItems(reservation);
                    reservation.getLines().forEach(line ->
                            stock.get(line.getProductId()).commit(line.getQuantity()));
                    reservation.markCommitted();
                    log.info("Committed stock for confirmed order {}", event.orderId());
                });
    }

    /** A new product was added in product-service: start tracking its stock. */
    @Transactional
    public void createStockItem(ProductCreatedEvent event) {
        if (itemRepository.existsById(event.productId())) {
            return;
        }
        itemRepository.save(new InventoryItem(event.productId(), event.sku(), event.initialStock()));
        log.info("Tracking stock for new product {} ({} units)", event.productId(), event.initialStock());
    }

    @Transactional
    public StockResponse restock(Long productId, int quantity) {
        InventoryItem item = itemRepository.findAllForUpdate(List.of(productId)).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No stock record for product " + productId));
        item.restock(quantity);
        return StockResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> findAll() {
        return itemRepository.findAllByOrderByProductIdAsc().stream().map(StockResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StockResponse find(Long productId) {
        return itemRepository.findById(productId).map(StockResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No stock record for product " + productId));
    }

    private Map<Long, InventoryItem> lockItems(StockReservation reservation) {
        List<Long> productIds = reservation.getLines().stream().map(ReservationLine::getProductId).toList();
        return itemRepository.findAllForUpdate(productIds).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
    }
}
