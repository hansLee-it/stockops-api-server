package com.stockops.environment.retention;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
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
}
