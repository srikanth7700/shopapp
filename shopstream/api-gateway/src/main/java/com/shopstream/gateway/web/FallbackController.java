package com.shopstream.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * When a circuit is open, or a downstream service is down or too slow, the
 * CircuitBreaker filter forwards the request here instead of failing with a
 * connection error.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "title", "Service Unavailable",
                "detail", service + " is not available right now. Please try again in a few seconds."));
    }
}
