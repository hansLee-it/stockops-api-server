package com.stockops.notification.sms;

/**
 * Result object returned by SmsGateway.send().
 * Encapsulates the outcome of an SMS dispatch attempt.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record SmsResult(
        /**
         * Whether the SMS was accepted for delivery.
         */
        boolean success,
        /**
         * Provider-assigned message identifier (null on failure).
         */
        String messageId,
        /**
         * Human-readable error description when success is false.
         */
        String errorMessage
) {
    /**
     * Factory for a successful send result.
     *
     * @param messageId provider message id
     * @return successful result
     */
    public static SmsResult ok(final String messageId) {
        return new SmsResult(true, messageId, null);
    }

    /**
     * Factory for a failed send result.
     *
     * @param errorMessage failure description
     * @return failed result
     */
    public static SmsResult failure(final String errorMessage) {
        return new SmsResult(false, null, errorMessage);
    }
}