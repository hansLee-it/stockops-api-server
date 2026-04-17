package com.stockops.controller;

import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.dto.SensorHistoryResponse;
import com.stockops.service.EnvironmentQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Environment query API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/environment")
public class EnvironmentQueryController {

    private final EnvironmentQueryService environmentQueryService;

    /**
     * Creates the controller.
     *
     * @param environmentQueryService environment query service
     */
    public EnvironmentQueryController(final EnvironmentQueryService environmentQueryService) {
        this.environmentQueryService = environmentQueryService;
    }

    /**
     * Returns aggregated environment dashboard data.
     *
     * @return dashboard response
     */
    @GetMapping("/dashboard")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(environmentQueryService.getDashboard());
    }

    /**
     * Returns recent alerts for the requested time window.
     *
     * @param days optional time window in days, defaults to 30
     * @return newest-first alerts
     */
    @GetMapping("/alerts")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<List<SensorAlertResponse>> getAlerts(@RequestParam(required = false) final Integer days) {
        return ResponseEntity.ok(environmentQueryService.getAlerts(days));
    }

    /**
     * Returns sensor reading history for the requested sensor and time window.
     *
     * @param sensorId sensor device identifier
     * @param days optional time window in days, defaults to 30
     * @return oldest-first time-series readings
     */
    @GetMapping("/history")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<List<SensorHistoryResponse>> getHistory(
            @RequestParam final Long sensorId,
            @RequestParam(required = false) final Integer days) {
        return ResponseEntity.ok(environmentQueryService.getHistory(sensorId, days));
    }
}
