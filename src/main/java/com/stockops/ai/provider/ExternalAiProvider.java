package com.stockops.ai.provider;

import com.stockops.ai.forecast.ForecastContext;
import com.stockops.ai.forecast.ForecastResult;

/**
 * Contract for external AI providers (Gemini, OpenAI, Claude, etc.).
 * Each implementation communicates with a specific third-party AI API
 * to produce demand forecasts from inventory context.
 *
 * <p>Implementations must be safe for concurrent use and must handle
 * their own authentication, retries, and error mapping.</p>
 *
 * @author StockOps Team
 * @since 2.0
 * @see GeminiAiProvider
 * @see ExternalAiProviderRegistry
 */
public interface ExternalAiProvider {

    /**
     * Returns the unique provider identifier.
     * Used by {@link ExternalAiProviderRegistry} to look up the correct provider.
     *
     * @return provider id string (e.g. "gemini", "openai", "claude")
     */
    String getProviderId();

    /**
     * Sends the inventory context to the external AI API and returns a forecast result.
     * <p>
     * When the provider is disabled or the API key is missing, implementations
     * should return a placeholder result rather than throwing.
     *
     * @param request forecast request containing inventory context
     * @return forecast response from the external AI
     * @throws IllegalStateException if the provider is misconfigured
     */
    ForecastResult predict(ExternalAiForecastRequest request);
}
