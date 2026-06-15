package com.stockops.environment.ingestion;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable Sensimul live telemetry payload.
 * Mirrors the simulator's JSON schema for MQTT sensor messages.
 *
 * @param siteId site identifier
 * @param sensorId sensor identifier
 * @param sensorType sensor type code
 * @param valueKind value kind code
 * @param value measured value
 * @param unit measurement unit
 * @param status sensor status
 * @param timestamp event timestamp in ISO-8601/RFC3339 format
 * @param sequenceId monotonically increasing sequence id
 * @param schemaVersion schema version marker
 * @author StockOps Team
 * @since 1.0
 */
public record SensimulPayload(
        @JsonProperty("site_id") String siteId,
        @JsonProperty("sensor_id") String sensorId,
        @JsonProperty("sensor_type") String sensorType,
        @JsonProperty("value_kind") String valueKind,
        double value,
        String unit,
        String status,
        String timestamp,
        @JsonProperty("sequence_id") long sequenceId,
        @JsonProperty("schema_version") String schemaVersion) {
}
