package com.stockops.exception;

/**
 * Thrown when a user is authenticated but outside the required data scope.
 *
 * @author StockOps Team
 * @since 2.0
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message exception message
     */
    public ForbiddenException(final String message) {
        super(message);
    }
}
