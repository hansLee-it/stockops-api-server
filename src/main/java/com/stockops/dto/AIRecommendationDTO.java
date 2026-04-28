package com.stockops.dto;

import com.stockops.entity.ai.AIRecommendationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * AI reorder recommendation response payload.
 *
 * @param id recommendation identifier
 * @param businessDate recommendation business date
 * @param productId product identifier
 * @param productName product name
 * @param productBarcode product barcode
 * @param centerId center identifier
 * @param warehouseId warehouse identifier
 * @param status recommendation lifecycle state
 * @param currentStockQuantity latest available stock used by the snapshot
 * @param safetyStockQuantity product safety stock used by the snapshot
 * @param recommendedQuantity recommended reorder quantity
 * @param sevenDayForecastQuantity forecasted demand for the next seven days
 * @param leadTimeDays expected lead time in days
 * @param leadTimeDemandQuantity forecasted demand during lead time
 * @param trailingSevenDayAverage trailing seven-day average demand
 * @param sameWeekdayAverage same-weekday lookback average demand
 * @param weightedDailyDemand weighted daily demand estimate
 * @param demandEventCount confirmed outbound event count used by the forecast
 * @param insufficientHistory whether the product lacked confirmed outbound history
 * @param explanationSummary deterministic explanation string for UI/API consumers
 * @param approvedPurchaseOrderId linked draft purchase-order id after approval
 * @param approvedPurchaseOrderNumber linked draft purchase-order number after approval
 * @param approvedAt recommendation approval timestamp
 * @param approvedByUserId approving user id
 * @param modelVersion identifier of the forecast model that produced this recommendation
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @author StockOps Team
 * @since 2.0
 */
public record AIRecommendationDTO(
        Long id,
        LocalDate businessDate,
        Long productId,
        String productName,
        String productBarcode,
        Long centerId,
        Long warehouseId,
        AIRecommendationStatus status,
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
        boolean insufficientHistory,
        String explanationSummary,
        Long approvedPurchaseOrderId,
        String approvedPurchaseOrderNumber,
        Instant approvedAt,
        Long approvedByUserId,
        String modelVersion,
        Instant createdAt,
        Instant updatedAt) {
}
