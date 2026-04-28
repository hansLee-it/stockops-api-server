package com.stockops.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * Product update request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record UpdateProductRequest(
        String name,
        String description,
        String category,
        Long categoryId,
        String unit,
        Boolean expiryManaged,
        @DecimalMin("0") BigDecimal defaultPrice,
        @Min(0) Integer safetyStockQuantity) {
}
