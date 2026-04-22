package com.stockops.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fill-rate analytics response.
 *
 * @param summary aggregate totals for the requested filter window
 * @param rows per-product scoped fill-rate rows
 * @author StockOps Team
 * @since 2.0
 */
public record FillRateReportResponse(
        FillRateSummary summary,
        List<FillRateRow> rows
) {

    /**
     * Summary totals for fill rate.
     *
     * @param rowCount number of scoped rows
     * @param purchaseOrderCount total purchase-order count
     * @param requestedQuantity total requested quantity
     * @param acceptedQuantity total accepted quantity
     * @param cancelledQuantity total cancelled quantity
     * @param shippedQuantity total shipped quantity
     * @param acceptanceRate accepted/requested ratio
     * @param shippedFillRate shipped/requested ratio
     */
    public record FillRateSummary(
            int rowCount,
            int purchaseOrderCount,
            int requestedQuantity,
            int acceptedQuantity,
            int cancelledQuantity,
            int shippedQuantity,
            BigDecimal acceptanceRate,
            BigDecimal shippedFillRate
    ) {
    }

    /**
     * Detailed fill-rate row.
     *
     * @param productId product identifier
     * @param productName product display name
     * @param centerId center identifier
     * @param centerName center display name
     * @param warehouseId warehouse identifier
     * @param warehouseName warehouse display name
     * @param purchaseOrderCount purchase-order count in range
     * @param requestedQuantity requested quantity in range
     * @param acceptedQuantity accepted quantity in range
     * @param cancelledQuantity cancelled quantity in range
     * @param shippedQuantity shipped quantity in range
     * @param acceptanceRate accepted/requested ratio for the row
     * @param shippedFillRate shipped/requested ratio for the row
     */
    public record FillRateRow(
            Long productId,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            int purchaseOrderCount,
            int requestedQuantity,
            int acceptedQuantity,
            int cancelledQuantity,
            int shippedQuantity,
            BigDecimal acceptanceRate,
            BigDecimal shippedFillRate
    ) {
    }
}
