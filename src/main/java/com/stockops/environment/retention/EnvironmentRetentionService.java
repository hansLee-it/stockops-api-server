package com.stockops.environment.retention;

import com.stockops.repository.EnvironmentAlertNotificationRepository;
import com.stockops.repository.EnvironmentAlertRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges aged environment alert events using UTC cutoffs derived from retention settings.
 * Live sensor measurements are no longer persisted, so only alert events are retained and purged.
 *
 * @author StockOps Team
 * @since 1.0
 * @see EnvironmentAlertRepository
 */
@Service
public class EnvironmentRetentionService {

    private final EnvironmentAlertRepository environmentAlertRepository;
    private final EnvironmentAlertNotificationRepository alertNotificationRepository;
    private final RetentionProperties retentionProperties;

    /**
     * Purges environment alerts older than the configured UTC retention cutoff.
     *
     * @return number of deleted alerts
     */
    @Transactional
    public int purgeOldAlerts() {
        return purgeOldAlerts(calculateCutoff());
    }

    /**
     * Purges all retained environment alert events and returns a combined execution summary.
     *
     * @return aggregate purge result
     */
    public PurgeResult purgeAll() {
        final Instant startedAt = Instant.now();
        final Instant cutoff = calculateCutoff();
        final int notificationsDeleted = purgeOldNotifications(cutoff);
        final int alertsDeleted = purgeOldAlerts(cutoff);
        final Duration duration = Duration.between(startedAt, Instant.now());

        // readingsDeleted is retained as 0 for backward-compatible reporting; raw readings are no longer stored.
        // Notification rows count toward the alert total because they are alert-event records.
        return new PurgeResult(0, alertsDeleted + notificationsDeleted, cutoff, duration);
    }

    private Instant calculateCutoff() {
        return Instant.now().minus(Duration.ofDays(retentionProperties.getRetentionDays()));
    }

    private int purgeOldAlerts(final Instant cutoff) {
        return environmentAlertRepository.deleteByCreatedAtBefore(cutoff);
    }

    /**
     * Purges terminal (SENT/FAILED/SUPERSEDED) outbox rows older than the cutoff.
     * PENDING rows are never purged so undelivered notifications are not lost.
     *
     * @param cutoff UTC cutoff
     * @return deleted row count
     */
    @Transactional
    public int purgeOldNotifications(final Instant cutoff) {
        return alertNotificationRepository.deleteTerminalBefore(cutoff);
    }

    public EnvironmentRetentionService(final EnvironmentAlertRepository environmentAlertRepository,
                                       final EnvironmentAlertNotificationRepository alertNotificationRepository,
                                       final RetentionProperties retentionProperties) {
        this.environmentAlertRepository = environmentAlertRepository;
        this.alertNotificationRepository = alertNotificationRepository;
        this.retentionProperties = retentionProperties;
    }
}
