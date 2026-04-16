package com.stockops.integration.sensimul;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Sensimul HTTP adapter.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ConfigurationProperties(prefix = "stockops.sensimul")
public class SensimulProperties {

    private String baseUrl = "http://localhost:18080";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(15);

    /**
     * Returns the Sensimul base URL.
     *
     * @return Sensimul base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Updates the Sensimul base URL.
     *
     * @param baseUrl Sensimul base URL
     */
    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the outbound connection timeout.
     *
     * @return connect timeout
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Updates the outbound connection timeout.
     *
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Returns the response read timeout.
     *
     * @return read timeout
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Updates the response read timeout.
     *
     * @param readTimeout read timeout
     */
    public void setReadTimeout(final Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Returns the base URL without a trailing slash.
     *
     * @return normalized base URL
     */
    public String getNormalizedBaseUrl() {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
