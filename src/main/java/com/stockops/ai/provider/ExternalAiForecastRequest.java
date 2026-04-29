package com.stockops.ai.provider;

import com.stockops.ai.forecast.ForecastContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request payload sent to an external AI provider.
 * Contains all inventory context needed for the AI to produce a demand forecast.
 *
 * @param productId product identifier
 * @param centerId center identifier
 * @param warehouseId warehouse identifier
 * @param businessDate the business date for which the forecast is generated
 * @param currentStockQuantity latest available stock at the scope
 * @param safetyStockQuantity product safety stock threshold
 * @param demandHistory chronological demand data points
 * @param leadTimeDays effective lead-time in days
 * @param forecastHorizonDays number of days ahead to forecast
 * @author StockOps Team
 * @since 2.0
 * @see ExternalAiProvider#predict
 */
public record ExternalAiForecastRequest(
        Long productId,
        Long centerId,
        Long warehouseId,
        LocalDate businessDate,
        int currentStockQuantity,
        int safetyStockQuantity,
        List<DemandPoint> demandHistory,
        int leadTimeDays,
        int forecastHorizonDays) {

    /**
     * One day of confirmed outbound demand.
     *
     * @param date date of the demand observation
     * @param quantity total confirmed outbound quantity on that date
     */
    public record DemandPoint(LocalDate date, int quantity) {
    }

    /**
     * Builds an ExternalAiForecastRequest from the internal {@link ForecastContext}.
     *
     * @param context the internal forecast context
     * @return external AI request ready to send to a provider
     */
    public static ExternalAiForecastRequest fromContext(final ForecastContext context) {
        final List<DemandPoint> demandPoints = context.demandHistory().stream()
                .map(dp -> new DemandPoint(dp.businessDate(), dp.confirmedOutboundQuantity()))
                .toList();
        return new ExternalAiForecastRequest(
                context.productId(),
                context.centerId(),
                context.warehouseId(),
                context.businessDate(),
                context.currentStockQuantity(),
                context.safetyStockQuantity(),
                demandPoints,
                context.leadTimeInfo().resolvedLeadTimeDays(),
                context.parameters().forecastHorizonDays());
    }
}
