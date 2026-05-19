package com.stockops.environment.retention;

import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorReadingRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges aged environment monitoring data using UTC cutoffs derived from retention settings.
 * Storage remains UTC in the database, while presentation-specific timezone conversion is delegated elsewhere.
 *
 * @author StockOps Team
 * @since 1.0
 * @see SensorReadingRepository
 * @see EnvironmentAlertRepository
 */
@Service
public class EnvironmentRetentionService {

    private final SensorReadingRepository sensorReadingRepository;
    private final EnvironmentAlertRepository environmentAlertRepository;
    private final RetentionProperties retentionProperties;

    /**
     * Purges sensor readings older than the configured UTC retention cutoff.
     *
     * @return number of deleted sensor readings
     */
    @Transactional
    public int purgeOldReadings() {
        return purgeOldReadings(calculateCutoff());
    }

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
     * Purges all retained environment monitoring history and returns a combined execution summary.
     *
     * @return aggregate purge result
     */
    public PurgeResult purgeAll() {
        final Instant startedAt = Instant.now();
        final Instant cutoff = calculateCutoff();
        final int readingsDeleted = purgeOldReadings(cutoff);
        final int alertsDeleted = purgeOldAlerts(cutoff);
        final Duration duration = Duration.between(startedAt, Instant.now());

        return new PurgeResult(readingsDeleted, alertsDeleted, cutoff, duration);
    }

    private Instant calculateCutoff() {
        return Instant.now().minus(Duration.ofDays(retentionProperties.getRetentionDays()));
    }

    private int purgeOldReadings(final Instant cutoff) {
        return sensorReadingRepository.deleteByRecordedAtBefore(cutoff);
    }

    private int purgeOldAlerts(final Instant cutoff) {
        return environmentAlertRepository.deleteByCreatedAtBefore(cutoff);
    }

    public EnvironmentRetentionService(final SensorReadingRepository sensorReadingRepository, final EnvironmentAlertRepository environmentAlertRepository, final RetentionProperties retentionProperties) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.environmentAlertRepository = environmentAlertRepository;
        this.retentionProperties = retentionProperties;
    }
}
