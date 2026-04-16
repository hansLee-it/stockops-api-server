package com.stockops.integration.sensimul;

/**
 * Parsed live sensor topic identifiers.
 *
 * @param siteId Sensimul site identifier
 * @param sensorId Sensimul sensor identifier
 * @author StockOps Team
 * @since 1.0
 */
public record ParsedSensorTopic(String siteId, String sensorId) {
}
