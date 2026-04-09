package com.stockops.dto;

import java.time.Instant;

/**
 * Stock adjustment response payload.
 * Includes inventory context, approval state, and audit-friendly operator details.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record StockAdjustmentDTO(
        Long id,
        Long inventoryId,
        String inventoryInfo,
        int beforeQuantity,
        int afterQuantity,
        int difference,
        Long reasonCodeId,
        String reasonCodeName,
        String note,
        String status,
        Long createdBy,
        String createdByName,
        Long approvedBy,
        String approvedByName,
        Instant createdAt,
        Instant updatedAt
) {
}
