package com.stockops.dto;

import java.time.Instant;

/**
 * Sensor reading history query response.
 *
 * @param sensorId sensor device identifier
 * @param value reading value
 * @param valueKind reading value kind
 * @param unit reading unit
 * @param status reading status
 * @param sequenceId reading sequence id
 * @param recordedAt reading timestamp in UTC
 * @author StockOps Team
 * @since 1.0
 */
public record SensorHistoryResponse(
        Long sensorId,
        Double value,
        String valueKind,
        String unit,
        String status,
        Long sequenceId,
        Instant recordedAt) {
}
