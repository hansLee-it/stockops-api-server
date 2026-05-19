package com.stockops.notification.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KakaoTalk configuration properties bound from application-pi.yml.
 * Supports future Kakao Business API integration (KakaoBizMsg or similar).
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoTalkConfig {

    /**
     * Whether KakaoTalk notifications are enabled.
     * When false, all KakaoTalk operations are stubbed (logged only).
     */
    private boolean enabled = false;

    /**
     * Kakao Business API key or access token.
     */
    private String apiKey;

    /**
     * Sender key registered in KakaoTalk Business Center.
     */
    private String senderKey;

    /**
     * Base URL for Kakao Business API endpoints.
     */
    private String baseUrl;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSenderKey() {
        return this.senderKey;
    }

    public void setSenderKey(final String senderKey) {
        this.senderKey = senderKey;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }
}