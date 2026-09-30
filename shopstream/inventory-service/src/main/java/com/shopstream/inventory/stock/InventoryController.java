package com.shopstream.inventory.stock;

import com.shopstream.inventory.web.Roles;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<StockResponse> all() {
        return inventoryService.findAll();
    }

    @GetMapping("/{productId}")
    public StockResponse one(@PathVariable Long productId) {
        return inventoryService.find(productId);
    }

    /** POST, not PUT: restocking twice adds twice, so the call is not idempotent. */
    @PostMapping("/{productId}/restock")
    public StockResponse restock(@RequestHeader(value = "X-User-Role", required = false) String role,
                                 @PathVariable Long productId,
                                 @Valid @RequestBody RestockRequest request) {
        Roles.requireAdmin(role);
        return inventoryService.restock(productId, request.quantity());
    }
}
