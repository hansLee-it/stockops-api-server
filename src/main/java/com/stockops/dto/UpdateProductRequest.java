package com.stockops.dto;

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
        String unit,
        Boolean expiryManaged) {
}
