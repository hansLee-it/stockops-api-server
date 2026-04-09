package com.stockops.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Stock adjustment approval request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record ApproveStockAdjustmentRequest(
        @NotNull Long adjustmentId,
        @NotNull boolean approved
) {
}
