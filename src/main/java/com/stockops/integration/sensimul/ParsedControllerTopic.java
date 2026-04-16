package com.stockops.integration.sensimul;

/**
 * Parsed live controller topic identifiers.
 *
 * @param siteId Sensimul site identifier
 * @param controllerId Sensimul controller identifier
 * @author StockOps Team
 * @since 1.0
 */
public record ParsedControllerTopic(String siteId, String controllerId) {
}
