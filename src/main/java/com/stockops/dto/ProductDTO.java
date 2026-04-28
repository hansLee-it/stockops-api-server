package com.stockops.dto;

import java.math.BigDecimal;
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
        Long categoryId,
        String categoryName,
        String unit,
        boolean expiryManaged,
        BigDecimal defaultPrice,
        Integer safetyStockQuantity,
        Instant createdAt,
        Instant updatedAt) {
}
