package com.stockops.dto;

import java.time.Instant;

/**
 * Product master response payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record ProductDTO(
        Long id,
        String barcode,
        String name,
        String description,
        String category,
        String unit,
        boolean expiryManaged,
        Instant createdAt,
        Instant updatedAt) {
}
