package com.shopstream.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void fallbackReturnsServiceUnavailableAndServiceSpecificDetails() {
        ResponseEntity<Map<String, Object>> response = controller.fallback("payment-service");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", 503)
                .containsEntry("title", "Service Unavailable")
                .containsEntry("detail", "payment-service is not available right now. Please try again in a few seconds.");
    }
}
