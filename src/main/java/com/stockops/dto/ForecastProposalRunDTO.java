package com.stockops.dto;

import com.stockops.entity.ai.ForecastProposalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * API payload for an intraday forecast proposal run.
 *
 * @author StockOps Team
 * @since 2.4
 */
public record ForecastProposalRunDTO(
        Long id,
        LocalDate businessDate,
        Integer runSlot,
        Instant runAt,
        Instant actionableUntil,
        boolean actionable,
        Long productId,
        String productName,
        String productBarcode,
        Long centerId,
        Long warehouseId,
        ForecastProposalStatus status,
        Integer currentStockQuantity,
        Integer safetyStockQuantity,
        Integer recommendedQuantity,
        Integer sevenDayForecastQuantity,
        Integer leadTimeDays,
        Integer leadTimeDemandQuantity,
        BigDecimal trailingSevenDayAverage,
        BigDecimal sameWeekdayAverage,
        BigDecimal weightedDailyDemand,
        Integer demandEventCount,
        String modelVersion,
        String explanationSummary,
        Long approvedPurchaseOrderId,
        String approvedPurchaseOrderNumber,
        Instant approvedAt,
        Long approvedByUserId,
        Instant rejectedAt,
        Long rejectedByUserId,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt) {
}
