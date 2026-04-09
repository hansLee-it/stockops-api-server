package com.stockops.exception;

/**
 * Thrown when a requested resource cannot be found.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message exception message
     */
    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
