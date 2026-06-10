package com.stockops.ai.bedrock;

import com.stockops.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-user in-memory rate limiter for AI RAG (Knowledge Base) queries.
 *
 * <p>Uses Bucket4j token buckets backed by a {@link ConcurrentHashMap}.
 * Each user gets an independent bucket, so users cannot affect each other's quotas.
 * The in-memory approach avoids a Redis dependency for AI-specific limits
 * while still using the same Bucket4j library already on the classpath.
 *
 * <p>Default limit: 10 requests per minute per user.
 * Override via {@code stockops.ai.rag.rate-limit.requests-per-minute}.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
public class AiRagRateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public AiRagRateLimiter(
            @Value("${stockops.ai.rag.rate-limit.requests-per-minute:10}")
            final int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    /**
     * Attempts to consume one token from the per-user bucket.
     *
     * @param userKey the authenticated user's identifier (email or username from JWT)
     * @throws RateLimitExceededException if the user has exceeded their quota this minute
     */
    public void checkRagLimit(final String userKey) {
        final Bucket bucket = buckets.computeIfAbsent(userKey, this::newBucket);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(
                    "AI Knowledge Base 쿼리 한도(" + requestsPerMinute + "회/분)를 초과했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private Bucket newBucket(final String ignored) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Returns the configured request-per-minute limit (for testing and health endpoints).
     */
    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }
}
