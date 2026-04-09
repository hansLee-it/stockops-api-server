package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;

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
        @NotBlank String unit,
        boolean expiryManaged) {
}
