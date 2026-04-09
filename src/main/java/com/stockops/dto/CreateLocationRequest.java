package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Location creation request payload.
 *
 * @param code unique location code
 * @param name location name
 * @param type location type
 * @param zone zone label
 * @param shelf shelf label
 * @param level level label
 * @author StockOps Team
 * @since 1.0
 */
public record CreateLocationRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String type,
        String zone,
        String shelf,
        String level
) {
}
