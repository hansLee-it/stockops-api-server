package com.stockops.dto;

import java.time.Instant;

/**
 * Reason code response payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record ReasonCodeDTO(
        Long id,
        String code,
        String name,
        String description,
        String category,
        Instant createdAt
) {
}
