package com.stockops.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Stock-aging analytics response.
 * Summarizes current scoped stock coverage using the analytics read model.
 *
 * @param summary aggregate totals for the requested filter window
 * @param rows per-product scoped stock-aging rows
 * @author StockOps Team
 * @since 2.0
 */
public record StockAgingReportResponse(
        StockAgingSummary summary,
        List<StockAgingRow> rows
) {

    /**
     * Summary totals for stock aging.
     *
     * @param rowCount number of scoped rows
     * @param totalAvailableQuantity total latest available stock quantity
     * @param zeroToThirtyQuantity quantity in the 0-30 day bucket
     * @param thirtyOneToSixtyQuantity quantity in the 31-60 day bucket
     * @param sixtyOneToNinetyQuantity quantity in the 61-90 day bucket
     * @param overNinetyQuantity quantity in the 90+ day bucket
     * @param noDemandQuantity quantity with no demand history in range
     */
    public record StockAgingSummary(
            int rowCount,
            int totalAvailableQuantity,
            int zeroToThirtyQuantity,
            int thirtyOneToSixtyQuantity,
            int sixtyOneToNinetyQuantity,
            int overNinetyQuantity,
            int noDemandQuantity
    ) {
    }

    /**
     * Detailed stock-aging row.
     *
     * @param productId product identifier
     * @param productName product display name
     * @param centerId center identifier
     * @param centerName center display name
     * @param warehouseId warehouse identifier
     * @param warehouseName warehouse display name
     * @param businessDate latest stock snapshot business date used for the row
     * @param availableQuantity latest available stock quantity
     * @param averageDailyDemand average confirmed outbound quantity per day in range
     * @param estimatedCoverageDays estimated days of stock coverage, or {@code null} when demand is zero
     * @param agingBucket coverage bucket label
     */
    public record StockAgingRow(
            Long productId,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            LocalDate businessDate,
            int availableQuantity,
            BigDecimal averageDailyDemand,
            BigDecimal estimatedCoverageDays,
            String agingBucket
    ) {
    }
}
