package com.stockops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMS configuration properties bound from application-pi.yml.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsConfig {

    private boolean enabled;
    private TwilioProperties twilio = new TwilioProperties();

    public static class TwilioProperties {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    
        public String getAccountSid() {
            return this.accountSid;
        }

        public void setAccountSid(final String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return this.authToken;
        }

        public void setAuthToken(final String authToken) {
            this.authToken = authToken;
        }

        public String getFromNumber() {
            return this.fromNumber;
        }

        public void setFromNumber(final String fromNumber) {
            this.fromNumber = fromNumber;
        }
}

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public TwilioProperties getTwilio() {
        return this.twilio;
    }

    public void setTwilio(final TwilioProperties twilio) {
        this.twilio = twilio;
    }
}