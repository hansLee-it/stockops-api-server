package com.stockops.ai.metrics;

import java.time.Instant;

/**
 * Immutable snapshot of a single AI provider call result.
 * Recorded by {@link AiCallMetrics} for audit logging and Micrometer instrumentation.
 *
 * @param requestId     unique identifier generated per facade invocation
 * @param provider      provider name: "bedrock", "vertex", or "none"
 * @param modelId       model reference used for generation (empty on failure)
 * @param useCase       caller-supplied use-case tag (e.g. RECOMMENDATION_EXPLANATION)
 * @param success       true if the provider returned a usable response
 * @param fallbackUsed  true if the primary provider failed and vertex fallback was used
 * @param failureReason short failure description or null on success
 * @param latencyMs     wall-clock milliseconds from facade entry to return/throw
 * @param inputTokens   number of input tokens consumed, or null if unavailable
 * @param outputTokens  number of output tokens generated, or null if unavailable
 * @param calledAt      UTC timestamp of the call
 * @author StockOps Team
 * @since 2.0
 */
public record AiCallRecord(
        String requestId,
        String provider,
        String modelId,
        String useCase,
        boolean success,
        boolean fallbackUsed,
        String failureReason,
        long latencyMs,
        Integer inputTokens,
        Integer outputTokens,
        Instant calledAt) {
}
