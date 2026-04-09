package com.stockops.dto;

import java.time.Instant;

/**
 * Location response payload.
 *
 * @param id location identifier
 * @param code unique location code
 * @param name location name
 * @param type location type
 * @param zone zone label
 * @param shelf shelf label
 * @param level level label
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record LocationDTO(
        Long id,
        String code,
        String name,
        String type,
        String zone,
        String shelf,
        String level,
        Instant createdAt,
        Instant updatedAt
) {
}
