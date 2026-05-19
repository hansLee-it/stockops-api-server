package com.stockops.environment.retention;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled retention batch for environment monitoring history.
 * Executes daily at 03:00 UTC and keeps timezone conversion concerns out of the persistence layer.
 *
 * @author StockOps Team
 * @since 1.0
 * @see EnvironmentRetentionService
 */
@Component
public class RetentionScheduler {

    private final EnvironmentRetentionService environmentRetentionService;
    private final RetentionProperties retentionProperties;

    /**
     * Runs the daily retention purge at 03:00 UTC.
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "UTC")
    public void purgeRetainedEnvironmentHistory() {
        if (!retentionProperties.isEnabled()) {
            log.info("Skipping environment retention batch job because stockops.retention.enabled=false");
            return;
        }

        final PurgeResult result = environmentRetentionService.purgeAll();
        log.info(
                "Environment retention batch job completed: cutoff={}, readingsDeleted={}, alertsDeleted={}, durationMs={}, batchSize={}",
                result.cutoffDate(),
                result.readingsDeleted(),
                result.alertsDeleted(),
                result.duration().toMillis(),
                retentionProperties.getBatchSize());
    }

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    public RetentionScheduler(final EnvironmentRetentionService environmentRetentionService, final RetentionProperties retentionProperties) {
        this.environmentRetentionService = environmentRetentionService;
        this.retentionProperties = retentionProperties;
    }
}
