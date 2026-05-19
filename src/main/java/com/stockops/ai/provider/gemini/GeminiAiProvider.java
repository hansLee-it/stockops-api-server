package com.stockops.ai.provider.gemini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.ai.forecast.ForecastResult;
import com.stockops.ai.provider.ExternalAiProvider;
import com.stockops.ai.provider.ExternalAiForecastRequest;
import com.stockops.entity.ai.AIRecommendationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of {@link ExternalAiProvider} for the Google Gemini API.
 * <p>
 * When enabled and an API key is configured, this provider formats an inventory
 * context prompt and would send it to the Gemini API. Until a live key is provided,
 * it logs the mock call and returns a placeholder {@link ForecastResult}.
 * <p>
 * Configuration:
 * <ul>
 *   <li>{@code stockops.ai.gemini.api-key} – Gemini API key (do NOT commit to source control)</li>
 *   <li>{@code stockops.ai.gemini.enabled} – whether the provider is active (default false)</li>
 *   <li>{@code stockops.ai.gemini.model-name} – model name (default "gemini-pro")</li>
 * </ul>
 *
 * @author StockOps Team
 * @since 2.0
 * @see ExternalAiProvider
 * @see GeminiAiProperties
 */
@Component
public class GeminiAiProvider implements ExternalAiProvider {

    private static final String PROVIDER_ID = "gemini";

    private final GeminiAiProperties properties;

    public GeminiAiProvider(final GeminiAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public ForecastResult predict(final ExternalAiForecastRequest request) {
        if (!properties.isEnabled() || properties.getApiKey().isBlank()) {
            log.warn("Gemini provider is disabled or API key is missing; returning placeholder result for productId={}",
                    request.productId());
            return placeholderResult(request);
        }

        final String prompt = buildPrompt(request);
        log.info("Gemini mock mode – would call API with model={} for productId={}. Prompt length={} chars",
                properties.getModelName(), request.productId(), prompt.length());

        return placeholderResult(request);
    }

    private String buildPrompt(final ExternalAiForecastRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append("You are an inventory forecasting assistant.\n\n");
        sb.append("Product ID: ").append(request.productId()).append("\n");
        sb.append("Center ID: ").append(request.centerId()).append("\n");
        sb.append("Warehouse ID: ").append(request.warehouseId()).append("\n");
        sb.append("Business Date: ").append(request.businessDate()).append("\n");
        sb.append("Current Stock: ").append(request.currentStockQuantity()).append("\n");
        sb.append("Safety Stock: ").append(request.safetyStockQuantity()).append("\n");
        sb.append("Lead Time Days: ").append(request.leadTimeDays()).append("\n");
        sb.append("Forecast Horizon Days: ").append(request.forecastHorizonDays()).append("\n\n");
        sb.append("Demand History (last 28 days):\n");

        final var history = request.demandHistory();
        final int start = Math.max(0, history.size() - 28);
        for (int i = start; i < history.size(); i++) {
            final var point = history.get(i);
            sb.append("  ").append(point.date()).append(": ").append(point.quantity()).append("\n");
        }

        sb.append("\nBased on this data, predict the recommended reorder quantity for the next ")
                .append(request.forecastHorizonDays()).append(" days.");

        return sb.toString();
    }

    private ForecastResult placeholderResult(final ExternalAiForecastRequest request) {
        return new ForecastResult(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                0,
                request.leadTimeDays(),
                0,
                0,
                0,
                true,
                0,
                AIRecommendationStatus.INSUFFICIENT_HISTORY,
                "Gemini external AI provider not yet integrated; placeholder result.",
                "gemini-stub");
    }

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);
}
