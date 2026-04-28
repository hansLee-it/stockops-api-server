package com.stockops.ai.forecast;

import com.stockops.entity.ai.AIRecommendationStatus;
import java.math.BigDecimal;

/**
 * Immutable output produced by {@link ForecastModel#computeForecast}.
 * <p>
 * Contains all forecast metrics needed to persist a snapshot and build
 * a reorder recommendation: weighted demand, lead-time demand, recommended
 * quantity, lifecycle status, and a human-readable explanation.
 *
 * @param trailingAverage average daily demand over the trailing window
 * @param sameWeekdayAverage average daily demand for the same weekday over the lookback weeks
 * @param weightedDailyDemand blended daily demand estimate (weighted combination of trailing + weekday)
 * @param sevenDayForecastQuantity total forecasted demand for the next 7 days
 * @param leadTimeDays effective lead-time in days
 * @param leadTimeDemandQuantity forecasted demand during lead time
 * @param historyDaysConsidered number of history days used by the model
 * @param demandEventCount total confirmed outbound event count in the history window
 * @param insufficientHistory true when the product lacked sufficient confirmed outbound history
 * @param recommendedQuantity suggested reorder quantity (may be 0)
 * @param status recommendation lifecycle state
 * @param explanationSummary deterministic explanation string for UI/API consumers
 * @param modelVersion identifier of the model that produced this result (e.g. "statistical")
 * @author StockOps Team
 * @since 2.0
 */
public record ForecastResult(
        BigDecimal trailingAverage,
        BigDecimal sameWeekdayAverage,
        BigDecimal weightedDailyDemand,
        int sevenDayForecastQuantity,
        int leadTimeDays,
        int leadTimeDemandQuantity,
        int historyDaysConsidered,
        int demandEventCount,
        boolean insufficientHistory,
        int recommendedQuantity,
        AIRecommendationStatus status,
        String explanationSummary,
        String modelVersion) {

    /**
     * Factory for an insufficient-history result when no confirmed outbound data exists.
     *
     * @param trailingAverage the trailing average (may be non-zero even without events)
     * @param leadTimeDays effective lead-time days
     * @param historyDaysConsidered history window size
     * @param modelVersion model identifier
     * @return a result indicating insufficient history with zero recommended quantity
     */
    public static ForecastResult insufficientHistory(final BigDecimal trailingAverage,
                                                     final int leadTimeDays,
                                                     final int historyDaysConsidered,
                                                     final String modelVersion) {
        return new ForecastResult(
                trailingAverage,
                BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP),
                0,
                leadTimeDays,
                0,
                historyDaysConsidered,
                0,
                true,
                0,
                AIRecommendationStatus.INSUFFICIENT_HISTORY,
                "No confirmed outbound history was available for deterministic forecasting.",
                modelVersion);
    }
}
