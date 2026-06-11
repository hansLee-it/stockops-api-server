package com.stockops.environment.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Shared recent sensor reading cache.
 *
 * <p>Implementations keep a short rolling window of normalized readings per sensor so
 * load-balanced API instances can serve live values without persisting raw measurements
 * in PostgreSQL. Cache failures must never break telemetry ingestion: write failures are
 * swallowed and read failures return empty results.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface SensorReadingCacheService {

    /**
     * Appends a normalized reading to the sensor's recent window and trims expired entries.
     *
     * @param snapshot normalized reading
     */
    void append(SensorReadingSnapshot snapshot);

    /**
     * Returns readings recorded within the requested window, oldest first.
     *
     * @param sensorDeviceId sensor device database identifier
     * @param window look-back window
     * @return readings within the window, empty when none are cached
     */
    List<SensorReadingSnapshot> readRecent(Long sensorDeviceId, Duration window);

    /**
     * Returns the most recent cached reading for the sensor, if any.
     *
     * @param sensorDeviceId sensor device database identifier
     * @return latest cached reading
     */
    Optional<SensorReadingSnapshot> latest(Long sensorDeviceId);
}
