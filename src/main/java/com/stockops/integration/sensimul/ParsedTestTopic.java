package com.stockops.integration.sensimul;

/**
 * Parsed Sensimul test topic identifiers.
 *
 * @param kind test topic kind, requests or results
 * @param siteId Sensimul site identifier
 * @param sensorId Sensimul sensor identifier
 * @author StockOps Team
 * @since 1.0
 */
public record ParsedTestTopic(String kind, String siteId, String sensorId) {
}
