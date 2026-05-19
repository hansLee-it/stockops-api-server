package com.stockops.service.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.config.MetricsConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled orchestration for analytics refresh and backfill jobs.
 * Both jobs run inside the current backend so the analytics layer stays close to operational data.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
public class AnalyticsAggregationScheduler {

    private final AnalyticsAggregationProperties properties;
    private final AnalyticsAggregationService analyticsAggregationService;
    private final MetricsConfig metricsConfig;

    /**
     * Refreshes the rolling analytics window used by BI and AI consumers.
     */
    @Scheduled(cron = "${stockops.analytics.refresh-cron:0 15 1 * * ?}", zone = "${stockops.analytics.business-zone:Asia/Seoul}")
    public void refreshIncrementalAnalytics() {
        if (!properties.isEnabled()) {
            log.info("Skipping analytics incremental refresh because stockops.analytics.enabled=false");
            return;
        }

        try {
            analyticsAggregationService.refreshIncrementalAggregates();
        } catch (final Exception e) {
            log.error(
                    "Analytics incremental refresh failed. Action required: inspect analytics tables "
                            + "and scheduler logs. error={}",
                    e.getMessage(),
                    e);
            metricsConfig.recordAnalyticsSchedulerFailure("incremental", e.getClass().getSimpleName());
        }
    }

    /**
     * Rebuilds the longer analytics history window on a slower cadence to support cold starts and drift correction.
     */
    @Scheduled(cron = "${stockops.analytics.backfill-cron:0 0 2 * * SUN}", zone = "${stockops.analytics.business-zone:Asia/Seoul}")
    public void backfillAnalyticsHistory() {
        if (!properties.isEnabled()) {
            log.info("Skipping analytics backfill because stockops.analytics.enabled=false");
            return;
        }

        try {
            analyticsAggregationService.backfillConfiguredHistory();
        } catch (final Exception e) {
            log.error(
                    "Analytics backfill failed. Action required: inspect analytics tables and "
                            + "scheduler logs. error={}",
                    e.getMessage(),
                    e);
            metricsConfig.recordAnalyticsSchedulerFailure("backfill", e.getClass().getSimpleName());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationScheduler.class);

    public AnalyticsAggregationScheduler(final AnalyticsAggregationProperties properties, final AnalyticsAggregationService analyticsAggregationService, final MetricsConfig metricsConfig) {
        this.properties = properties;
        this.analyticsAggregationService = analyticsAggregationService;
        this.metricsConfig = metricsConfig;
    }
}
