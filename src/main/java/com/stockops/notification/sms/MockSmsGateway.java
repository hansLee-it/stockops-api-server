package com.stockops.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock SMS gateway used when sms.enabled=false.
 * Logs the message instead of calling an external provider.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.enabled", havingValue = "false", matchIfMissing = true)
public class MockSmsGateway implements SmsGateway {

    @Override
    public SmsResult send(final String phoneNumber, final String message) {
        log.info("[MOCK SMS] To: {}, Message: {}", phoneNumber, message);
        return SmsResult.ok("MOCK-" + System.currentTimeMillis());
    }
}