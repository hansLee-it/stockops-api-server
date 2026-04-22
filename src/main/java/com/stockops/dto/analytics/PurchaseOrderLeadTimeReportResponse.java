package com.stockops.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Purchase-order lead-time analytics response.
 *
 * @param summary aggregate totals for the requested filter window
 * @param rows per-product scoped lead-time rows
 * @author StockOps Team
 * @since 2.0
 */
public record PurchaseOrderLeadTimeReportResponse(
        PurchaseOrderLeadTimeSummary summary,
        List<PurchaseOrderLeadTimeRow> rows
) {

    /**
     * Summary totals for purchase-order lead time.
     *
     * @param rowCount number of scoped rows
     * @param purchaseOrderCount total purchase-order count
     * @param leadTimeSampleCount total lead-time sample count
     * @param totalLeadTimeHours summed lead-time hours
     * @param averageLeadTimeHours average lead-time hours across sampled orders
     */
    public record PurchaseOrderLeadTimeSummary(
            int rowCount,
            int purchaseOrderCount,
            int leadTimeSampleCount,
            long totalLeadTimeHours,
            BigDecimal averageLeadTimeHours
    ) {
    }

    /**
     * Detailed purchase-order lead-time row.
     *
     * @param productId product identifier
     * @param productName product display name
     * @param centerId center identifier
     * @param centerName center display name
     * @param warehouseId warehouse identifier
     * @param warehouseName warehouse display name
     * @param purchaseOrderCount purchase-order count in range
     * @param leadTimeSampleCount lead-time sample count in range
     * @param totalLeadTimeHours summed lead-time hours in range
     * @param averageLeadTimeHours average lead-time hours for the row
     */
    public record PurchaseOrderLeadTimeRow(
            Long productId,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            int purchaseOrderCount,
            int leadTimeSampleCount,
            long totalLeadTimeHours,
            BigDecimal averageLeadTimeHours
    ) {
    }
}
