package com.stockops.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled orchestration for analytics refresh and backfill jobs.
 * Both jobs run inside the current backend so the analytics layer stays close to operational data.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsAggregationScheduler {

    private final AnalyticsAggregationProperties properties;
    private final AnalyticsAggregationService analyticsAggregationService;

    /**
     * Refreshes the rolling analytics window used by BI and AI consumers.
     */
    @Scheduled(cron = "${stockops.analytics.refresh-cron:0 15 1 * * ?}", zone = "${stockops.analytics.business-zone:Asia/Seoul}")
    public void refreshIncrementalAnalytics() {
        if (!properties.isEnabled()) {
            log.info("Skipping analytics incremental refresh because stockops.analytics.enabled=false");
            return;
        }

        analyticsAggregationService.refreshIncrementalAggregates();
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

        analyticsAggregationService.backfillConfiguredHistory();
    }
}
