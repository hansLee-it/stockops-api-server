package com.stockops.dto;

import com.stockops.entity.SensorType;
import java.time.Instant;

/**
 * Sensor device response payload.
 *
 * @param id sensor device identifier
 * @param name display name
 * @param siteId Sensimul site identifier
 * @param sensorId Sensimul sensor identifier
 * @param sensorType sensor type
 * @param location business location description
 * @param mqttTopic canonical Sensimul MQTT topic
 * @param sourceChannel upstream source channel value
 * @param active whether the sensor is active
 * @param deleted whether the sensor is soft-deleted
 * @param warnMin warning lower bound (null when unset)
 * @param warnMax warning upper bound
 * @param critMin critical lower bound
 * @param critMax critical upper bound
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record SensorDeviceResponse(
        Long id,
        String name,
        String siteId,
        String sensorId,
        SensorType sensorType,
        String location,
        String mqttTopic,
        String sourceChannel,
        boolean active,
        boolean deleted,
        Double warnMin,
        Double warnMax,
        Double critMin,
        Double critMax,
        Instant createdAt,
        Instant updatedAt) {
}
