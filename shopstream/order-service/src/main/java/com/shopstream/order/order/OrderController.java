package com.shopstream.order.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns 201 with status PENDING right away. The rest of the saga happens
     * asynchronously; the client polls GET /api/orders/{id} to follow it.
     * (Alternatives to polling: Server-Sent Events or WebSockets.)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@RequestHeader("X-User-Id") Long userId,
                               @RequestHeader("X-User-Email") String userEmail,
                               @Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(userId, userEmail, request);
    }

    @GetMapping
    public List<OrderResponse> myOrders(@RequestHeader("X-User-Id") Long userId) {
        return orderService.findForUser(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse one(@RequestHeader("X-User-Id") Long userId,
                             @RequestHeader(value = "X-User-Role", required = false) String role,
                             @PathVariable Long id) {
        return orderService.findOne(id, userId, "ADMIN".equals(role));
    }
}
