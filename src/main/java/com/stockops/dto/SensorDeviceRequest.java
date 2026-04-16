package com.stockops.dto;

import com.stockops.entity.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Sensor device create/update request payload.
 *
 * @param siteId Sensimul site identifier
 * @param sensorId Sensimul sensor identifier
 * @param sensorType sensor type
 * @param location business location description
 * @param mqttTopic canonical Sensimul MQTT topic for the sensor
 * @param sourceChannel upstream source channel value
 * @author StockOps Team
 * @since 1.0
 */
public record SensorDeviceRequest(
        @NotBlank String siteId,
        @NotBlank String sensorId,
        @NotNull SensorType sensorType,
        @NotBlank String location,
        @NotBlank @Pattern(regexp = "^sensimul/sites/[^/]+/sensors/[^/]+$") String mqttTopic,
        @NotBlank String sourceChannel) {
}
