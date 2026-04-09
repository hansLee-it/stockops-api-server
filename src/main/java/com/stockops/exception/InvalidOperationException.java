package com.stockops.exception;

/**
 * Raised when an inventory operation is not valid for the current input or state.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class InvalidOperationException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message validation message
     */
    public InvalidOperationException(final String message) {
        super(message);
    }
}
