package com.stockops.dto;

import java.time.LocalDate;

/**
 * Transaction PDF export filter payload.
 *
 * @param startDate inclusive local-date range start
 * @param endDate inclusive local-date range end
 * @param centerId center identifier filter
 * @param warehouseId warehouse identifier filter
 * @param status transaction status filter from the management page
 * @author StockOps Team
 * @since 1.0
 */
public record TransactionReportFilter(
        LocalDate startDate,
        LocalDate endDate,
        Long centerId,
        Long warehouseId,
        String status
) {
}
