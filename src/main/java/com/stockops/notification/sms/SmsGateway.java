package com.stockops.notification.sms;

/**
 * Contract for SMS providers. Implementations handle the protocol details
 * (Twilio REST, AWS SNS, etc.) while callers receive a uniform result.
 *
 * <p>Implementations must be safe for concurrent use.</p>
 *
 * @author StockOps Team
 * @since 1.0
 * @see TwilioSmsGateway
 * @see MockSmsGateway
 */
public interface SmsGateway {

    /**
     * Dispatches a single SMS message.
     *
     * @param phoneNumber destination phone number in E.164 format (e.g. +821012345678)
     * @param message    text content (max 1600 characters for multi-part)
     * @return send result indicating success or failure with details
     * @throws IllegalArgumentException if phoneNumber or message is blank
     */
    SmsResult send(String phoneNumber, String message);
}