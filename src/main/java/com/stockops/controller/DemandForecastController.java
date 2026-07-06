package com.stockops.controller;

import com.stockops.service.DemandForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/demand-forecast")
public class DemandForecastController {

    private final DemandForecastService forecastService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<List<Map<String, Object>>> getForecast(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(forecastService.generateForecast(productId, days));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<List<Map<String, Object>>> getLowStockProducts() {
        return ResponseEntity.ok(forecastService.getLowStockProducts());
    }

    public DemandForecastController(final DemandForecastService forecastService) {
        this.forecastService = forecastService;
    }

}