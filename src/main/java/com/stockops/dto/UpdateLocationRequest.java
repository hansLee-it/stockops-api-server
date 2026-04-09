package com.stockops.dto;

/**
 * Location update request payload.
 *
 * @param name location name
 * @param type location type
 * @param zone zone label
 * @param shelf shelf label
 * @param level level label
 * @author StockOps Team
 * @since 1.0
 */
public record UpdateLocationRequest(
        String name,
        String type,
        String zone,
        String shelf,
        String level
) {
}
