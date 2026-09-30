package com.shopstream.inventory.stock;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RestockRequest(@Min(1) @Max(10000) int quantity) {
}
