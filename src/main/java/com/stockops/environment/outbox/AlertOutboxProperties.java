package com.stockops.environment.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the environment alert notification outbox sender.
 *
 * @author StockOps Team
 * @since 2.3
 */
@ConfigurationProperties(prefix = "stockops.environment.alert-outbox")
public class AlertOutboxProperties {

    private boolean enabled = true;
    private Duration pollInterval = Duration.ofSeconds(15);
    private int batchSize = 20;
    private int maxAttempts = 5;

    /**
     * Returns whether the scheduled sender runs.
     *
     * @return enabled flag
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the scheduled sender runs.
     *
     * @param enabled enabled flag
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the sender poll interval.
     *
     * @return poll interval
     */
    public Duration getPollInterval() {
        return pollInterval;
    }

    /**
     * Sets the sender poll interval.
     *
     * @param pollInterval poll interval
     */
    public void setPollInterval(final Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    /**
     * Returns the maximum rows claimed per poll.
     *
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the maximum rows claimed per poll.
     *
     * @param batchSize batch size
     */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Returns the delivery attempt limit before a row is marked FAILED.
     *
     * @return max attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets the delivery attempt limit.
     *
     * @param maxAttempts max attempts
     */
    public void setMaxAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
