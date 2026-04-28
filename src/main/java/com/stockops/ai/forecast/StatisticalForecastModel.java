package com.stockops.ai.forecast;

import com.stockops.entity.ai.AIRecommendationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Deterministic statistical forecast model extracted from the original
 * {@code AIRecommendationService} engine.
 * <p>
 * Algorithm: trailing N-day average (weight W1) + same-weekday lookback average
 * (weight W2) → weighted daily demand → lead-time demand → reorder quantity.
 * All arithmetic is preserved exactly from the original implementation.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service("statisticalForecastModel")
public class StatisticalForecastModel implements ForecastModel {

    private static final String MODEL_ID = "statistical";

    @Override
    public String getModelId() {
        return MODEL_ID;
    }

    @Override
    public ForecastResult computeForecast(final ForecastContext context) {
        final ForecastContext.ForecastParameters params = context.parameters();
        final LocalDate businessDate = context.businessDate();

        final Map<LocalDate, ForecastContext.DemandDataPoint> demandByDate = indexDemandByDate(context.demandHistory());

        final int trailingDays = params.trailingAverageDays();
        int trailingQuantityTotal = 0;
        for (int dayOffset = trailingDays; dayOffset >= 1; dayOffset--) {
            trailingQuantityTotal += quantityForDate(demandByDate, businessDate.minusDays(dayOffset));
        }
        final BigDecimal trailingAverage = BigDecimal.valueOf(trailingQuantityTotal)
                .divide(BigDecimal.valueOf(trailingDays), 2, RoundingMode.HALF_UP);

        int demandEventCount = context.demandHistory().stream()
                .mapToInt(ForecastContext.DemandDataPoint::confirmedOutboundEventCount)
                .sum();
        if (demandEventCount == 0) {
            return ForecastResult.insufficientHistory(
                    trailingAverage,
                    context.leadTimeInfo().resolvedLeadTimeDays(),
                    params.forecastHistoryDays(),
                    MODEL_ID);
        }

        final int forecastHorizonDays = params.forecastHorizonDays();
        final List<BigDecimal> dailyForecasts = new java.util.ArrayList<>();
        BigDecimal weekdayAverageAccumulator = BigDecimal.ZERO;
        for (int forecastOffset = 0; forecastOffset < forecastHorizonDays; forecastOffset++) {
            final LocalDate targetDate = businessDate.plusDays(forecastOffset);
            final BigDecimal weekdayAverage = sameWeekdayAverage(demandByDate, targetDate, params.sameWeekdayLookbackWeeks());
            weekdayAverageAccumulator = weekdayAverageAccumulator.add(weekdayAverage);
            final BigDecimal weightedDemand = trailingAverage.multiply(params.trailingAverageWeight())
                    .add(weekdayAverage.multiply(params.weekdayLookbackWeight()))
                    .setScale(2, RoundingMode.HALF_UP);
            dailyForecasts.add(weightedDemand);
        }

        final BigDecimal sameWeekdayAverage = weekdayAverageAccumulator
                .divide(BigDecimal.valueOf(forecastHorizonDays), 2, RoundingMode.HALF_UP);
        final BigDecimal weightedDailyDemand = dailyForecasts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(forecastHorizonDays), 2, RoundingMode.HALF_UP);
        final int sevenDayForecastQuantity = dailyForecasts.stream()
                .map(value -> value.setScale(0, RoundingMode.HALF_UP).intValue())
                .reduce(0, Integer::sum);
        final int leadTimeDays = context.leadTimeInfo().resolvedLeadTimeDays();
        final int leadTimeDemandQuantity = weightedDailyDemand
                .multiply(BigDecimal.valueOf(leadTimeDays))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        final int recommendedQuantity = Math.max(
                leadTimeDemandQuantity + context.safetyStockQuantity() - context.currentStockQuantity(), 0);
        final AIRecommendationStatus status = recommendedQuantity > 0
                ? AIRecommendationStatus.READY_FOR_APPROVAL
                : AIRecommendationStatus.NO_ACTION;
        final String explanationSummary = String.format(
                "Forecast=%s/day (70%% trailing-7 avg %s + 30%% weekday avg %s); leadTimeDays=%d; currentStock=%d; safetyStock=%d; recommended=%d",
                weightedDailyDemand,
                trailingAverage,
                sameWeekdayAverage,
                leadTimeDays,
                context.currentStockQuantity(),
                context.safetyStockQuantity(),
                recommendedQuantity);

        return new ForecastResult(
                trailingAverage,
                sameWeekdayAverage,
                weightedDailyDemand,
                sevenDayForecastQuantity,
                leadTimeDays,
                leadTimeDemandQuantity,
                params.forecastHistoryDays(),
                demandEventCount,
                false,
                recommendedQuantity,
                status,
                explanationSummary,
                MODEL_ID);
    }

    private Map<LocalDate, ForecastContext.DemandDataPoint> indexDemandByDate(
            final List<ForecastContext.DemandDataPoint> demandHistory) {
        final Map<LocalDate, ForecastContext.DemandDataPoint> indexed = new HashMap<>();
        for (ForecastContext.DemandDataPoint point : demandHistory) {
            indexed.put(point.businessDate(), point);
        }
        return indexed;
    }

    private BigDecimal sameWeekdayAverage(
            final Map<LocalDate, ForecastContext.DemandDataPoint> demandByDate,
            final LocalDate targetDate,
            final int lookbackWeeks) {
        BigDecimal total = BigDecimal.ZERO;
        int sampleCount = 0;
        for (int week = 1; week <= lookbackWeeks; week++) {
            final LocalDate lookbackDate = targetDate.minusWeeks(week);
            total = total.add(BigDecimal.valueOf(quantityForDate(demandByDate, lookbackDate)));
            sampleCount++;
        }
        if (sampleCount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(sampleCount), 2, RoundingMode.HALF_UP);
    }

    private int quantityForDate(
            final Map<LocalDate, ForecastContext.DemandDataPoint> demandByDate,
            final LocalDate businessDate) {
        return Optional.ofNullable(demandByDate.get(businessDate))
                .map(ForecastContext.DemandDataPoint::confirmedOutboundQuantity)
                .orElse(0);
    }
}
