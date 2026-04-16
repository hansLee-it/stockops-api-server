package com.stockops.exception;

/**
 * Thrown when a request conflicts with the current resource state.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class ConflictException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message exception message
     */
    public ConflictException(final String message) {
        super(message);
    }
}
