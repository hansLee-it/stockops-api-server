package com.stockops.controller;

import com.stockops.dto.AcknowledgeAlertRequest;
import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.service.EnvironmentQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * Acknowledges an environment alert, recording the administrator's handling note.
     *
     * @param id alert identifier
     * @param request acknowledgement request carrying the handling note
     * @return the updated alert
     */
    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<SensorAlertResponse> acknowledgeAlert(
            @PathVariable final Long id,
            @Valid @RequestBody(required = false) final AcknowledgeAlertRequest request) {
        final String note = request == null ? null : request.note();
        return ResponseEntity.ok(environmentQueryService.acknowledgeAlert(id, note));
    }
}
