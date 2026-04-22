package com.stockops.dto.analytics;

import java.util.List;

/**
 * Expiry-waste analytics response.
 *
 * @param summary aggregate totals for the requested filter window
 * @param rows per-product scoped expiry-waste rows
 * @author StockOps Team
 * @since 2.0
 */
public record ExpiryWasteReportResponse(
        ExpiryWasteSummary summary,
        List<ExpiryWasteRow> rows
) {

    /**
     * Summary totals for expiry waste.
     *
     * @param rowCount number of scoped rows
     * @param quarantinedQuantity total quarantined quantity
     * @param quarantinedLotCount total quarantined lot count
     */
    public record ExpiryWasteSummary(
            int rowCount,
            int quarantinedQuantity,
            int quarantinedLotCount
    ) {
    }

    /**
     * Detailed expiry-waste row.
     *
     * @param productId product identifier
     * @param productName product display name
     * @param centerId center identifier
     * @param centerName center display name
     * @param warehouseId warehouse identifier
     * @param warehouseName warehouse display name
     * @param quarantinedQuantity quarantined quantity in range
     * @param quarantinedLotCount quarantined lot count in range
     */
    public record ExpiryWasteRow(
            Long productId,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            int quarantinedQuantity,
            int quarantinedLotCount
    ) {
    }
}
