package com.stockops.dto;

import java.time.Instant;
import java.util.List;

/**
 * Recent sensor readings query response served from the shared Redis cache.
 *
 * @param sensorId sensor device database identifier
 * @param windowMinutes effective look-back window in minutes
 * @param readings readings within the window, oldest first
 * @author StockOps Team
 * @since 1.0
 */
public record RecentSensorReadingsResponse(
        Long sensorId,
        int windowMinutes,
        List<ReadingPoint> readings) {

    /**
     * Single cached reading point.
     *
     * @param value measured value
     * @param valueKind measured value kind
     * @param unit measurement unit
     * @param status reported sensor status
     * @param recordedAt measurement timestamp in UTC
     * @param sequenceId payload sequence id
     */
    public record ReadingPoint(
            Double value,
            String valueKind,
            String unit,
            String status,
            Instant recordedAt,
            Long sequenceId) {
    }
}
