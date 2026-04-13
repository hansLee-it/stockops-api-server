package com.stockops.dto;

/**
 * Inventory PDF export filter payload.
 *
 * @param search free-text search applied to product and location fields
 * @param status inventory status filter
 * @param centerId center identifier filter
 * @param warehouseId warehouse identifier filter
 * @author StockOps Team
 * @since 1.0
 */
public record InventoryReportFilter(
        String search,
        String status,
        Long centerId,
        Long warehouseId
) {
}
