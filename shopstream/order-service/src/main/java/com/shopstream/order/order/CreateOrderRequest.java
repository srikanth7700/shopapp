package com.shopstream.order.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The client only sends product ids and quantities, never prices.
 * Prices always come from product-service: never trust a price from the browser.
 */
public record CreateOrderRequest(@NotEmpty @Size(max = 20) List<@Valid Line> items) {

    public record Line(@NotNull Long productId, @Min(1) @Max(99) int quantity) {
    }
}
