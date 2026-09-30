package com.shopstream.payment.payment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only API. Payments are only ever created by the saga (Kafka), never by a direct HTTP call. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> myPayments(@RequestHeader("X-User-Id") Long userId) {
        return paymentService.findForUser(userId);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse forOrder(@RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                    @PathVariable Long orderId) {
        return paymentService.findForOrder(orderId, userId, "ADMIN".equals(role));
    }
}
