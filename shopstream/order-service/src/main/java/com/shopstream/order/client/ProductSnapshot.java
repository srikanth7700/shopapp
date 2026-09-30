package com.shopstream.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Only the product fields order-service needs. Unknown JSON fields are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSnapshot(Long id, String name, BigDecimal price, boolean active) {
}
