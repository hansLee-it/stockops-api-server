package com.stockops.service.analytics;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for analytics refresh and backfill jobs.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "stockops.analytics")
public class AnalyticsAggregationProperties {

    private boolean enabled = true;

    private String businessZone = "Asia/Seoul";

    private int incrementalLookbackDays = 30;

    private int backfillDays = 365;
}
