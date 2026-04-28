package com.stockops.controller;

import com.stockops.dto.CategoryInventoryDTO;
import com.stockops.service.CategoryInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for category-based inventory aggregation.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class CategoryInventoryController {

    private final CategoryInventoryService categoryInventoryService;

    public CategoryInventoryController(final CategoryInventoryService categoryInventoryService) {
        this.categoryInventoryService = categoryInventoryService;
    }

    @GetMapping("/by-category")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<CategoryInventoryDTO> getCategoryInventory(
            @RequestParam final Long categoryId) {
        return ResponseEntity.ok(categoryInventoryService.getCategoryInventorySummary(categoryId));
    }
}