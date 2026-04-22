package com.stockops.dto.analytics;

import java.time.LocalDate;

/**
 * Shared analytics query filter.
 * Applies optional center, warehouse, and business-date bounds to BI/reporting surfaces.
 *
 * @param from inclusive business-date start
 * @param to inclusive business-date end
 * @param centerId optional center identifier
 * @param warehouseId optional warehouse identifier
 * @author StockOps Team
 * @since 2.0
 */
public record AnalyticsQueryFilter(
        LocalDate from,
        LocalDate to,
        Long centerId,
        Long warehouseId
) {
}
