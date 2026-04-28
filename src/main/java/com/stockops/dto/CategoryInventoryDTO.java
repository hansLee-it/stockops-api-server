package com.stockops.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Category-level inventory aggregation response.
 * Aggregates inventory data across all products in a category and its subcategories.
 *
 * @param categoryId root category identifier
 * @param categoryName root category display name
 * @param totalProducts total number of distinct products across all categories in the tree
 * @param totalQuantity total inventory quantity across all products
 * @param totalValue total inventory value (quantity × unit price) across all products
 * @param subcategories per-subcategory breakdown
 * @author StockOps Team
 * @since 2.0
 */
public record CategoryInventoryDTO(
        Long categoryId,
        String categoryName,
        long totalProducts,
        int totalQuantity,
        BigDecimal totalValue,
        List<SubcategoryInventoryDTO> subcategories) {
}