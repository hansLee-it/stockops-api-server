package com.stockops.ai.forecast;

/**
 * Pluggable seam for demand-forecast models.
 * <p>
 * Implementations encapsulate a specific forecasting strategy (statistical,
 * external AI, etc.) behind a uniform contract so that
 * {@code AIRecommendationService} and {@code DemandForecastService} can
 * delegate without knowing the internals.
 *
 * @author StockOps Team
 * @since 2.0
 * @see StatisticalForecastModel
 * @see ExternalAIForecastAdapter
 */
public interface ForecastModel {

    /**
     * Unique identifier for this model, used as a selector key in API requests.
     *
     * @return model identifier (e.g. "statistical", "external")
     */
    String getModelId();

    /**
     * Computes a demand forecast for the supplied context.
     * <p>
     * Implementations must be deterministic given identical inputs so that
     * results remain explainable and reproducible.
     *
     * @param context forecast inputs (demand history, stock levels, lead-time, parameters)
     * @return forecast outputs (weighted demand, recommended quantity, status, etc.)
     */
    ForecastResult computeForecast(ForecastContext context);
}
