package com.stockops.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stockout-rate analytics response.
 *
 * @param summary aggregate totals for the requested filter window
 * @param rows per-product scoped stockout rows
 * @author StockOps Team
 * @since 2.0
 */
public record StockoutRateReportResponse(
        StockoutRateSummary summary,
        List<StockoutRateRow> rows
) {

    /**
     * Summary totals for stockout reporting.
     *
     * @param rowCount number of scoped rows
     * @param observedDayCount total observed business days across rows
     * @param stockoutDayCount total stockout business days across rows
     * @param overallStockoutRate stockout day ratio across the scoped dataset
     */
    public record StockoutRateSummary(
            int rowCount,
            int observedDayCount,
            int stockoutDayCount,
            BigDecimal overallStockoutRate
    ) {
    }

    /**
     * Detailed stockout row.
     *
     * @param productId product identifier
     * @param productName product display name
     * @param centerId center identifier
     * @param centerName center display name
     * @param warehouseId warehouse identifier
     * @param warehouseName warehouse display name
     * @param observedDayCount observed business-day count
     * @param stockoutDayCount business-day count with zero available stock
     * @param stockoutRate stockout ratio for the row
     * @param latestAvailableQuantity latest available quantity in range
     */
    public record StockoutRateRow(
            Long productId,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            int observedDayCount,
            int stockoutDayCount,
            BigDecimal stockoutRate,
            int latestAvailableQuantity
    ) {
    }
}
