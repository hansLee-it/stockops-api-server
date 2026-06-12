package com.stockops.environment.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Sensimul SQS telemetry subscriber.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ConfigurationProperties(prefix = "stockops.sqs-ingestion")
public class SqsIngestionProperties {

    private boolean enabled;
    private String queueUrl;
    private String region = "ap-northeast-2";
    private int maxMessages = 10;
    private int waitTimeSeconds = 20;
    private int visibilityTimeoutSeconds = 30;
    private long errorBackoffMillis = 5000;

    /**
     * Returns whether SQS ingestion is enabled.
     *
     * @return true when subscriber should start
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether SQS ingestion is enabled.
     *
     * @param enabled enable flag
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the SQS queue URL.
     *
     * @return queue URL
     */
    public String getQueueUrl() {
        return queueUrl;
    }

    /**
     * Sets the SQS queue URL.
     *
     * @param queueUrl queue URL
     */
    public void setQueueUrl(final String queueUrl) {
        this.queueUrl = queueUrl;
    }

    /**
     * Returns the AWS region.
     *
     * @return AWS region id
     */
    public String getRegion() {
        return region;
    }

    /**
     * Sets the AWS region.
     *
     * @param region AWS region id
     */
    public void setRegion(final String region) {
        this.region = region;
    }

    /**
     * Returns max messages per poll.
     *
     * @return max message count
     */
    public int getMaxMessages() {
        return maxMessages;
    }

    /**
     * Sets max messages per poll.
     *
     * @param maxMessages max message count
     */
    public void setMaxMessages(final int maxMessages) {
        this.maxMessages = maxMessages;
    }

    /**
     * Returns long polling wait time.
     *
     * @return wait time in seconds
     */
    public int getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    /**
     * Sets long polling wait time.
     *
     * @param waitTimeSeconds wait time in seconds
     */
    public void setWaitTimeSeconds(final int waitTimeSeconds) {
        this.waitTimeSeconds = waitTimeSeconds;
    }

    /**
     * Returns visibility timeout.
     *
     * @return visibility timeout in seconds
     */
    public int getVisibilityTimeoutSeconds() {
        return visibilityTimeoutSeconds;
    }

    /**
     * Sets visibility timeout.
     *
     * @param visibilityTimeoutSeconds visibility timeout in seconds
     */
    public void setVisibilityTimeoutSeconds(final int visibilityTimeoutSeconds) {
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    }

    /**
     * Returns poll loop backoff after an error.
     *
     * @return backoff in milliseconds
     */
    public long getErrorBackoffMillis() {
        return errorBackoffMillis;
    }

    /**
     * Sets poll loop backoff after an error.
     *
     * @param errorBackoffMillis backoff in milliseconds
     */
    public void setErrorBackoffMillis(final long errorBackoffMillis) {
        this.errorBackoffMillis = errorBackoffMillis;
    }
}
