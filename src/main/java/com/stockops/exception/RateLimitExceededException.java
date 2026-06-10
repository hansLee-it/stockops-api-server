package com.stockops.exception;

/**
 * Thrown when a caller exceeds the allowed rate limit for an operation.
 * Maps to HTTP 429 Too Many Requests via {@link GlobalExceptionHandler}.
 *
 * @author StockOps Team
 * @since 2.0
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(final String message) {
        super(message);
    }
}
