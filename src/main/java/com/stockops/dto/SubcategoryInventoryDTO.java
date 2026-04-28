package com.stockops.dto;

import java.math.BigDecimal;

/**
 * Subcategory-level inventory breakdown within a category aggregation.
 *
 * @param categoryId subcategory identifier
 * @param categoryName subcategory display name
 * @param productCount number of products in this subcategory
 * @param quantity total inventory quantity across all products
 * @param value total inventory value (quantity × unit price)
 * @author StockOps Team
 * @since 2.0
 */
public record SubcategoryInventoryDTO(
        Long categoryId,
        String categoryName,
        long productCount,
        int quantity,
        BigDecimal value) {
}