package com.stockops.dto;

import com.stockops.entity.SensorType;
import java.time.Instant;
import java.util.List;

/**
 * Environment dashboard query response.
 *
 * @param totalSensors total registered non-deleted sensors
 * @param activeSensors active sensor count
 * @param normalCount normal/info alert count
 * @param warningCount warning alert count
 * @param dangerCount danger/critical alert count
 * @param latestReadings latest reading snapshot per sensor
 * @author StockOps Team
 * @since 1.0
 */
public record DashboardResponse(
        long totalSensors,
        long activeSensors,
        long normalCount,
        long warningCount,
        long dangerCount,
        List<LatestReadingSummary> latestReadings) {

    /**
     * Latest reading summary displayed on the environment dashboard.
     *
     * @param sensorId sensor device identifier
     * @param sensorName sensor display name
     * @param sensorType sensor type
     * @param warehouseName warehouse the sensor is installed in (null when unmapped)
     * @param value latest measured value
     * @param valueKind measured value kind
     * @param unit measured unit
     * @param status latest sensor status
     * @param recordedAt sensor event timestamp in UTC
     */
    public record LatestReadingSummary(
            Long sensorId,
            String sensorName,
            SensorType sensorType,
            String warehouseName,
            Double value,
            String valueKind,
            String unit,
            String status,
            Instant recordedAt) {
    }
}
