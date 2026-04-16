package com.stockops.dto;

import com.stockops.entity.AlertSeverity;
import java.time.Instant;

/**
 * Environment alert query response.
 *
 * @param id alert identifier
 * @param sensorId related sensor device identifier
 * @param sensorName related sensor name
 * @param alertType alert type code
 * @param severity alert severity
 * @param message alert message
 * @param acknowledged whether the alert has been acknowledged
 * @param acknowledgedAt acknowledgement timestamp
 * @param acknowledgedBy acknowledgement actor
 * @param createdAt alert creation timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record SensorAlertResponse(
        Long id,
        Long sensorId,
        String sensorName,
        String alertType,
        AlertSeverity severity,
        String message,
        boolean acknowledged,
        Instant acknowledgedAt,
        String acknowledgedBy,
        Instant createdAt) {
}
