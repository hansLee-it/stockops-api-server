package com.stockops.environment.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Redis-backed recent sensor reading cache.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ConfigurationProperties(prefix = "stockops.sensor-cache")
public class SensorCacheProperties {

    private Duration recentWindow = Duration.ofMinutes(10);
    private Duration keyTtl = Duration.ofMinutes(15);
    private String keyPrefix = "stockops:sensor:readings";

    /**
     * Returns the retention window for recent readings.
     *
     * @return recent reading retention window
     */
    public Duration getRecentWindow() {
        return recentWindow;
    }

    /**
     * Sets the retention window for recent readings.
     *
     * @param recentWindow recent reading retention window
     */
    public void setRecentWindow(final Duration recentWindow) {
        this.recentWindow = recentWindow;
    }

    /**
     * Returns the Redis key expiration, slightly above the retention window
     * so keys for inactive sensors clean themselves up.
     *
     * @return key TTL
     */
    public Duration getKeyTtl() {
        return keyTtl;
    }

    /**
     * Sets the Redis key expiration.
     *
     * @param keyTtl key TTL
     */
    public void setKeyTtl(final Duration keyTtl) {
        this.keyTtl = keyTtl;
    }

    /**
     * Returns the Redis key prefix for per-sensor reading sorted sets.
     *
     * @return key prefix
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * Sets the Redis key prefix.
     *
     * @param keyPrefix key prefix
     */
    public void setKeyPrefix(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
