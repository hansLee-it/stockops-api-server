package com.stockops.controller;

import com.stockops.security.ScopeGuard;
import com.stockops.service.CenterInventoryAggregationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Center inventory aggregation.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/centers")
public class CenterInventoryController {

    private final CenterInventoryAggregationService centerInventoryService;
    private final ScopeGuard scopeGuard;

    @GetMapping("/{centerId}/inventory")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public Map<String, Object> getCenterInventory(@PathVariable Long centerId) {
        scopeGuard.assertCenterAccess(centerId);
        return centerInventoryService.getCenterInventorySummary(centerId);
    }

    public CenterInventoryController(final CenterInventoryAggregationService centerInventoryService,
                                     final ScopeGuard scopeGuard) {
        this.centerInventoryService = centerInventoryService;
        this.scopeGuard = scopeGuard;
    }

}
