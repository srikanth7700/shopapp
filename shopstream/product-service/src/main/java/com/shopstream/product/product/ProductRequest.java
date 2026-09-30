package com.shopstream.product.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Z0-9-]+", message = "use upper-case letters, digits and dashes") String sku,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 60) String category,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 255) String imageUrl,
        /* Only used when creating: how many units inventory-service should start with. */
        @Min(0) Integer initialStock) {
}
