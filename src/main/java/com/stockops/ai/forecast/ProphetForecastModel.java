package com.stockops.ai.forecast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.entity.ai.AIRecommendationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Prophet-powered forecast model that delegates demand prediction to the
 * Python AI service via {@link AiForecastClient}.
 * <p>
 * When the AI service is unavailable or the circuit breaker is open,
 * this model transparently falls back to the local
 * {@link StatisticalForecastModel} so that recommendation generation never
 * breaks. The fallback preserves full explainability because the statistical
 * model produces deterministic snapshots just like the primary path.
 *
 * @author StockOps Team
 * @since 2.0
 * @see AiForecastClient
 * @see StatisticalForecastModel
 */
@Service("prophetForecastModel")
public class ProphetForecastModel implements ForecastModel {

    private static final String MODEL_ID = "prophet";

    private final AiForecastClient aiForecastClient;
    private final ForecastModel fallbackModel;

    public ProphetForecastModel(
            final AiForecastClient aiForecastClient,
            @Qualifier("statisticalForecastModel") final ForecastModel fallbackModel) {
        this.aiForecastClient = aiForecastClient;
        this.fallbackModel = fallbackModel;
    }

    @Override
    public String getModelId() {
        return MODEL_ID;
    }

    @Override
    public ForecastResult computeForecast(final ForecastContext context) {
        final AiForecastClient.AiForecastResponse response = aiForecastClient.getForecast(
                context.productId(),
                context.parameters().forecastHorizonDays());

        if (response == null || response.forecast() == null || response.forecast().isEmpty()) {
            log.warn("Prophet forecast unavailable for productId={}, falling back to statistical model", context.productId());
            final ForecastResult fallback = fallbackModel.computeForecast(context);
            return new ForecastResult(
                    fallback.trailingAverage(),
                    fallback.sameWeekdayAverage(),
                    fallback.weightedDailyDemand(),
                    fallback.sevenDayForecastQuantity(),
                    fallback.leadTimeDays(),
                    fallback.leadTimeDemandQuantity(),
                    fallback.historyDaysConsidered(),
                    fallback.demandEventCount(),
                    fallback.insufficientHistory(),
                    fallback.recommendedQuantity(),
                    fallback.status(),
                    "Prophet fallback: " + fallback.explanationSummary(),
                    MODEL_ID);
        }

        return mapProphetResponseToResult(context, response);
    }

    private ForecastResult mapProphetResponseToResult(
            final ForecastContext context,
            final AiForecastClient.AiForecastResponse response) {

        final List<AiForecastClient.AiForecastResponse.ForecastPoint> points = response.forecast();
        final int horizonDays = context.parameters().forecastHorizonDays();

        final List<Double> yhatValues = points.stream()
                .limit(horizonDays)
                .map(AiForecastClient.AiForecastResponse.ForecastPoint::yhat)
                .toList();

        if (yhatValues.isEmpty()) {
            log.warn("Prophet returned empty forecast for productId={}, falling back to statistical model", context.productId());
            final ForecastResult fallback = fallbackModel.computeForecast(context);
            return new ForecastResult(
                    fallback.trailingAverage(),
                    fallback.sameWeekdayAverage(),
                    fallback.weightedDailyDemand(),
                    fallback.sevenDayForecastQuantity(),
                    fallback.leadTimeDays(),
                    fallback.leadTimeDemandQuantity(),
                    fallback.historyDaysConsidered(),
                    fallback.demandEventCount(),
                    fallback.insufficientHistory(),
                    fallback.recommendedQuantity(),
                    fallback.status(),
                    "Prophet fallback: " + fallback.explanationSummary(),
                    MODEL_ID);
        }

        final double totalYhat = yhatValues.stream().mapToDouble(Double::doubleValue).sum();
        final double avgYhat = totalYhat / yhatValues.size();

        final BigDecimal weightedDailyDemand = BigDecimal.valueOf(avgYhat)
                .setScale(2, RoundingMode.HALF_UP);
        final BigDecimal trailingAverage = weightedDailyDemand;
        final BigDecimal sameWeekdayAverage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        final int sevenDayForecastQuantity = yhatValues.stream()
                .map(y -> (int) Math.round(y))
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
                "Prophet forecast=%s/day ( Prophet AI model ); leadTimeDays=%d; currentStock=%d; safetyStock=%d; recommended=%d",
                weightedDailyDemand,
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
                context.parameters().forecastHistoryDays(),
                context.demandHistory().size(),
                false,
                recommendedQuantity,
                status,
                explanationSummary,
                MODEL_ID);
    }

    private static final Logger log = LoggerFactory.getLogger(ProphetForecastModel.class);
}
