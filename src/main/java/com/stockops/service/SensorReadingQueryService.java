package com.stockops.service;

import com.stockops.dto.RecentSensorReadingsResponse;
import com.stockops.entity.SensorDevice;
import com.stockops.environment.cache.SensorCacheProperties;
import com.stockops.environment.cache.SensorReadingCacheService;
import com.stockops.environment.cache.SensorReadingSnapshot;
import com.stockops.exception.ResourceNotFoundException;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stockops.repository.SensorDeviceRepository;

/**
 * Read-only recent sensor reading query service.
 *
 * <p>Validates the sensor master record, caps the requested window to the configured
 * retention window, and serves readings from the shared Redis cache only. When the cache
 * holds no recent readings the response carries an empty list.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class SensorReadingQueryService {

    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorReadingCacheService sensorReadingCacheService;
    private final SensorCacheProperties sensorCacheProperties;

    /**
     * Creates the service.
     *
     * @param sensorDeviceRepository sensor device repository
     * @param sensorReadingCacheService shared recent reading cache
     * @param sensorCacheProperties sensor cache properties
     */
    public SensorReadingQueryService(
            final SensorDeviceRepository sensorDeviceRepository,
            final SensorReadingCacheService sensorReadingCacheService,
            final SensorCacheProperties sensorCacheProperties) {
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.sensorReadingCacheService = sensorReadingCacheService;
        this.sensorCacheProperties = sensorCacheProperties;
    }

    /**
     * Returns the sensor's cached readings within the requested window.
     *
     * @param sensorId sensor device database identifier
     * @param minutes requested look-back window in minutes; defaults to the configured
     *     retention window when absent or invalid, and is capped at that window
     * @return recent readings response, oldest first
     */
    @Transactional(readOnly = true)
    public RecentSensorReadingsResponse getRecentReadings(final Long sensorId, final Integer minutes) {
        final SensorDevice sensor = sensorDeviceRepository.findByIdAndDeletedFalse(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("센서를 찾을 수 없습니다: " + sensorId));

        final int windowMinutes = resolveWindowMinutes(minutes);
        final List<SensorReadingSnapshot> snapshots = sensorReadingCacheService
                .readRecent(sensor.getId(), Duration.ofMinutes(windowMinutes));

        final List<RecentSensorReadingsResponse.ReadingPoint> readings = snapshots.stream()
                .map(snapshot -> new RecentSensorReadingsResponse.ReadingPoint(
                        snapshot.value(),
                        snapshot.valueKind(),
                        snapshot.unit(),
                        snapshot.status(),
                        snapshot.recordedAt(),
                        snapshot.sequenceId()))
                .toList();
        return new RecentSensorReadingsResponse(sensor.getId(), windowMinutes, readings);
    }

    private int resolveWindowMinutes(final Integer requestedMinutes) {
        final int maxMinutes = Math.max(1, (int) sensorCacheProperties.getRecentWindow().toMinutes());
        if (requestedMinutes == null || requestedMinutes <= 0) {
            return maxMinutes;
        }
        return Math.min(requestedMinutes, maxMinutes);
    }
}
