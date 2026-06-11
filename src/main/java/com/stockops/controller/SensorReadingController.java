package com.stockops.controller;

import com.stockops.dto.RecentSensorReadingsResponse;
import com.stockops.service.SensorReadingQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recent sensor reading query API controller.
 * Serves live sensor values from the shared Redis cache so browser clients
 * never connect to MQTT directly.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/environment/sensors")
public class SensorReadingController {

    private final SensorReadingQueryService sensorReadingQueryService;

    /**
     * Creates the controller.
     *
     * @param sensorReadingQueryService recent reading query service
     */
    public SensorReadingController(final SensorReadingQueryService sensorReadingQueryService) {
        this.sensorReadingQueryService = sensorReadingQueryService;
    }

    /**
     * Returns the sensor's recent cached readings.
     *
     * @param sensorId sensor device identifier
     * @param minutes optional look-back window in minutes, capped at the configured retention window
     * @return recent readings, oldest first; empty when nothing is cached
     */
    @GetMapping("/{sensorId}/readings/recent")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<RecentSensorReadingsResponse> getRecentReadings(
            @PathVariable final Long sensorId,
            @RequestParam(required = false) final Integer minutes) {
        return ResponseEntity.ok(sensorReadingQueryService.getRecentReadings(sensorId, minutes));
    }
}
