package com.stockops.ai.forecast;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Immutable input context consumed by {@link ForecastModel#computeForecast}.
 * <p>
 * Carries all the data a forecast model needs: demand history, current stock,
 * lead-time statistics, safety stock, and configurable parameters.
 *
 * @param productId product identifier
 * @param centerId center identifier
 * @param warehouseId warehouse identifier
 * @param businessDate the business date for which the forecast is generated
 * @param currentStockQuantity latest available stock at the scope
 * @param safetyStockQuantity product safety stock threshold
 * @param demandHistory chronological demand data points from the analytics read model
 * @param leadTimeInfo lead-time statistics (observed or default)
 * @param parameters model tuning parameters (weights, horizons, lookback windows)
 * @author StockOps Team
 * @since 2.0
 */
public record ForecastContext(
        Long productId,
        Long centerId,
        Long warehouseId,
        LocalDate businessDate,
        int currentStockQuantity,
        int safetyStockQuantity,
        List<DemandDataPoint> demandHistory,
        LeadTimeInfo leadTimeInfo,
        ForecastParameters parameters) {

    /**
     * One day of confirmed outbound demand for a product/center/warehouse scope.
     *
     * @param businessDate date of the demand observation
     * @param confirmedOutboundQuantity total confirmed outbound quantity on that date
     * @param confirmedOutboundEventCount number of distinct outbound events on that date
     */
    public record DemandDataPoint(
            LocalDate businessDate,
            int confirmedOutboundQuantity,
            int confirmedOutboundEventCount) {
    }

    /**
     * Observed or default lead-time statistics for a product scope.
     *
     * @param totalLeadTimeHours cumulative lead-time hours across samples
     * @param sampleCount number of observed lead-time samples
     * @param defaultLeadTimeDays fallback lead-time in days when no samples exist
     */
    public record LeadTimeInfo(
            long totalLeadTimeHours,
            int sampleCount,
            int defaultLeadTimeDays) {

        /**
         * Resolves the effective lead-time in days.
         * Falls back to {@code defaultLeadTimeDays} when no samples are available.
         *
         * @return lead-time in days, at least 1
         */
        public int resolvedLeadTimeDays() {
            if (sampleCount <= 0) {
                return Math.max(defaultLeadTimeDays, 1);
            }
            final BigDecimal averageHours = BigDecimal.valueOf(totalLeadTimeHours)
                    .divide(BigDecimal.valueOf(sampleCount), 2, java.math.RoundingMode.HALF_UP);
            return Math.max(averageHours.divide(BigDecimal.valueOf(24), 0, java.math.RoundingMode.CEILING).intValue(), 1);
        }

        /**
         * Convenience factory for a default lead-time with no observed samples.
         *
         * @param defaultLeadTimeDays fallback days
         * @return lead-time info with zero samples
         */
        public static LeadTimeInfo defaultFor(final int defaultLeadTimeDays) {
            return new LeadTimeInfo((long) defaultLeadTimeDays * 24L, 1, defaultLeadTimeDays);
        }
    }

    /**
     * Tunable parameters that control the statistical forecast computation.
     *
     * @param trailingAverageDays number of trailing days for the moving average
     * @param sameWeekdayLookbackWeeks number of weeks to look back for same-weekday averaging
     * @param forecastHorizonDays number of days ahead to forecast
     * @param forecastHistoryDays total history window considered (for metadata)
     * @param trailingAverageWeight weight applied to the trailing average component (e.g. 0.70)
     * @param weekdayLookbackWeight weight applied to the same-weekday component (e.g. 0.30)
     */
    public record ForecastParameters(
            int trailingAverageDays,
            int sameWeekdayLookbackWeeks,
            int forecastHorizonDays,
            int forecastHistoryDays,
            BigDecimal trailingAverageWeight,
            BigDecimal weekdayLookbackWeight) {
    }
}
