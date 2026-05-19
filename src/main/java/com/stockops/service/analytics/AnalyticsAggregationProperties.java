package com.stockops.service.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for analytics refresh and backfill jobs.
 *
 * @author StockOps Team
 * @since 2.0
 */
@ConfigurationProperties(prefix = "stockops.analytics")
public class AnalyticsAggregationProperties {

    private boolean enabled = true;

    private String businessZone = "Asia/Seoul";

    private int incrementalLookbackDays = 30;

    private int backfillDays = 365;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getBusinessZone() {
        return this.businessZone;
    }

    public void setBusinessZone(final String businessZone) {
        this.businessZone = businessZone;
    }

    public int getIncrementalLookbackDays() {
        return this.incrementalLookbackDays;
    }

    public void setIncrementalLookbackDays(final int incrementalLookbackDays) {
        this.incrementalLookbackDays = incrementalLookbackDays;
    }

    public int getBackfillDays() {
        return this.backfillDays;
    }

    public void setBackfillDays(final int backfillDays) {
        this.backfillDays = backfillDays;
    }
}
