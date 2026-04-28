package com.stockops.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMS configuration properties bound from application-pi.yml.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsConfig {

    private boolean enabled;
    private TwilioProperties twilio = new TwilioProperties();

    @Data
    public static class TwilioProperties {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    }
}