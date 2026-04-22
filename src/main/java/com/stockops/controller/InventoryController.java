package com.stockops.controller;

import com.stockops.dto.InventoryDTO;
import com.stockops.dto.InventoryTransactionDTO;
import com.stockops.service.InventoryQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory query API controller.
 * Collection endpoints return only rows inside the caller's effective scope.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryService inventoryQueryService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<List<InventoryDTO>> getAllInventory(
            @RequestParam(required = false) final Long productId,
            @RequestParam(required = false) final Long locationId,
            @RequestParam(required = false) final Long lotId,
            @RequestParam(required = false) final Integer page,
            @RequestParam(required = false) final Integer size) {

        final List<InventoryDTO> inventory;
        if (productId != null) {
            inventory = inventoryQueryService.getInventoryByProduct(productId);
        } else if (locationId != null) {
            inventory = inventoryQueryService.getInventoryByLocation(locationId);
        } else if (lotId != null) {
            inventory = inventoryQueryService.getInventoryByLot(lotId);
        } else {
            inventory = inventoryQueryService.getAllInventory();
        }

        return ResponseEntity.ok(applyPagination(inventory, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<InventoryDTO> getInventory(@PathVariable final Long id) {
        return ResponseEntity.ok(inventoryQueryService.getInventoryById(id));
    }

    @GetMapping("/transactions")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<List<InventoryTransactionDTO>> getTransactions(
            @RequestParam(required = false) final Long productId,
            @RequestParam(required = false) final Long locationId,
            @RequestParam(required = false) final Long lotId,
            @RequestParam(required = false) final Integer page,
            @RequestParam(required = false) final Integer size) {
        final List<InventoryTransactionDTO> transactions = inventoryQueryService.getTransactionHistory(
                productId, locationId, lotId);
        return ResponseEntity.ok(applyPagination(transactions, page, size));
    }

    @GetMapping("/transactions/recent")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<List<InventoryTransactionDTO>> getRecentTransactions() {
        return ResponseEntity.ok(inventoryQueryService.getRecentTransactions(50));
    }

    private <T> List<T> applyPagination(final List<T> items, final Integer page, final Integer size) {
        if (page == null || size == null || size <= 0 || page < 0) {
            return items;
        }

        final int fromIndex = Math.min(page * size, items.size());
        final int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
