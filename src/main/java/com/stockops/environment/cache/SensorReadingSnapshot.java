package com.stockops.environment.cache;

import java.time.Instant;

/**
 * Normalized recent sensor reading stored in the shared Redis cache.
 * Compact JSON form of this record is the sorted-set member; {@code recordedAt}
 * epoch milliseconds is the sorted-set score.
 *
 * @param sensorDeviceId sensor device database identifier
 * @param siteId Sensimul site identifier
 * @param sensorId Sensimul external sensor identifier
 * @param sensorType sensor type code reported by the payload
 * @param valueKind measured value kind
 * @param value measured value
 * @param unit measurement unit
 * @param status reported sensor status
 * @param recordedAt measurement timestamp in UTC
 * @param sequenceId monotonically increasing payload sequence id
 * @author StockOps Team
 * @since 1.0
 */
public record SensorReadingSnapshot(
        Long sensorDeviceId,
        String siteId,
        String sensorId,
        String sensorType,
        String valueKind,
        Double value,
        String unit,
        String status,
        Instant recordedAt,
        Long sequenceId) {
}
