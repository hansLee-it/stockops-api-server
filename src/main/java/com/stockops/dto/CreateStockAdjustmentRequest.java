package com.stockops.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Stock adjustment creation request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record CreateStockAdjustmentRequest(
        @NotNull Long inventoryId,
        @NotNull @PositiveOrZero int newQuantity,
        @NotNull Long reasonCodeId,
        String note
) {
}
