package com.stockops.ai.forecast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.ai.provider.ExternalAiForecastRequest;
import com.stockops.ai.provider.ExternalAiProvider;
import com.stockops.ai.provider.ExternalAiProviderRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Gemini-powered forecast model that delegates demand prediction to the
 * {@link ExternalAiProviderRegistry} to locate the {@code gemini} provider.
 * <p>
 * When the Gemini provider is disabled or unavailable, this model transparently
 * falls back to the local {@link StatisticalForecastModel} so that recommendation
 * generation never breaks.
 *
 * @author StockOps Team
 * @since 2.0
 * @see ExternalAiProvider
 * @see GeminiAiProvider
 * @see StatisticalForecastModel
 */
@Service("geminiForecastModel")
public class GeminiForecastModel implements ForecastModel {

    private static final String MODEL_ID = "gemini";

    private final ExternalAiProviderRegistry providerRegistry;
    private final ForecastModel fallbackModel;

    public GeminiForecastModel(
            final ExternalAiProviderRegistry providerRegistry,
            @Qualifier("statisticalForecastModel") final ForecastModel fallbackModel) {
        this.providerRegistry = providerRegistry;
        this.fallbackModel = fallbackModel;
    }

    @Override
    public String getModelId() {
        return MODEL_ID;
    }

    @Override
    public ForecastResult computeForecast(final ForecastContext context) {
        final var providerOpt = providerRegistry.getProvider(MODEL_ID);
        if (providerOpt.isEmpty()) {
            log.warn("Gemini provider not registered; falling back to statistical model for productId={}",
                    context.productId());
            return wrapFallback(fallbackModel.computeForecast(context));
        }

        final var request = ExternalAiForecastRequest.fromContext(context);
        final var result = providerOpt.get().predict(request);

        if (result == null || result.insufficientHistory()) {
            log.warn("Gemini returned insufficient-history for productId={}; falling back to statistical model",
                    context.productId());
            return wrapFallback(fallbackModel.computeForecast(context));
        }

        return result;
    }

    private ForecastResult wrapFallback(final ForecastResult fallback) {
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
                "Gemini fallback: " + fallback.explanationSummary(),
                MODEL_ID);
    }

    private static final Logger log = LoggerFactory.getLogger(GeminiForecastModel.class);
}
