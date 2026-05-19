package com.stockops.environment.retention;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable retention settings for environment monitoring history cleanup.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ConfigurationProperties(prefix = "stockops.retention")
public class RetentionProperties {

    private int retentionDays = 30;

    private int batchSize = 1000;

    private boolean enabled = true;

    public int getRetentionDays() {
        return this.retentionDays;
    }

    public void setRetentionDays(final int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
