package com.stockops.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Product creation request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record CreateProductRequest(
        @NotBlank String barcode,
        @NotBlank String name,
        String description,
        String category,
        Long categoryId,
        @NotBlank String unit,
        boolean expiryManaged,
        @DecimalMin("0") BigDecimal defaultPrice,
        @Min(0) Integer safetyStockQuantity) {
}
