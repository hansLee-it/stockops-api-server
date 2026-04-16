package com.stockops.integration.sensimul;

/**
 * Raised when the Sensimul adapter cannot complete an outbound integration call.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class SensimulIntegrationException extends RuntimeException {

    /**
     * Creates the exception with a message.
     *
     * @param message exception message
     */
    public SensimulIntegrationException(final String message) {
        super(message);
    }

    /**
     * Creates the exception with a message and cause.
     *
     * @param message exception message
     * @param cause root cause
     */
    public SensimulIntegrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
