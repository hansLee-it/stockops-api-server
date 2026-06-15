package com.stockops.service;

import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.SensorDevice;
import com.stockops.entity.Warehouse;
import com.stockops.environment.cache.SensorReadingCacheService;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.security.CurrentUserProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only environment dashboard and alert query service.
 *
 * <p>Live sensor measurements are not stored in PostgreSQL; latest values come from the shared
 * Redis recent-reading cache, while the dashboard's normal/warning/danger view is derived from
 * the currently <em>active</em> environment alerts (unresolved and unacknowledged events).
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class EnvironmentQueryService {

    private static final int DEFAULT_DAYS = 30;

    private final SensorDeviceRepository sensorDeviceRepository;

    private final EnvironmentAlertRepository environmentAlertRepository;

    private final CurrentUserProvider currentUserProvider;

    private final SensorReadingCacheService sensorReadingCacheService;

    private final WarehouseRepository warehouseRepository;

    /**
     * Creates the service.
     *
     * @param sensorDeviceRepository sensor repository
     * @param environmentAlertRepository environment alert (event) repository
     * @param currentUserProvider current authenticated user provider
     * @param sensorReadingCacheService shared recent reading cache
     * @param warehouseRepository warehouse repository (resolves sensor warehouse display names)
     */
    public EnvironmentQueryService(
            final SensorDeviceRepository sensorDeviceRepository,
            final EnvironmentAlertRepository environmentAlertRepository,
            final CurrentUserProvider currentUserProvider,
            final SensorReadingCacheService sensorReadingCacheService,
            final WarehouseRepository warehouseRepository) {
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.environmentAlertRepository = environmentAlertRepository;
        this.currentUserProvider = currentUserProvider;
        this.sensorReadingCacheService = sensorReadingCacheService;
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * Returns aggregated dashboard data for the environment domain.
     *
     * <p>Latest readings come from the shared Redis recent-reading cache; sensors without a cached
     * reading are omitted. Normal/warning/danger counts are computed from active alerts: a sensor
     * with an active CRITICAL alert is dangerous, an active WARNING alert is a warning, and the
     * remainder of active sensors are normal.
     *
     * @return dashboard response
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        final List<SensorDevice> sensors = sensorDeviceRepository.findAll();
        final long totalSensors = sensors.size();
        final long activeSensors = sensors.stream().filter(SensorDevice::isActive).count();

        final List<EnvironmentAlert> activeAlerts = environmentAlertRepository.findByResolvedAtIsNullAndAcknowledgedFalse();
        final Set<Long> criticalSensors = activeAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .map(EnvironmentAlert::getSensorDeviceId)
                .collect(Collectors.toSet());
        final Set<Long> warningSensors = activeAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .map(EnvironmentAlert::getSensorDeviceId)
                .filter(sensorId -> !criticalSensors.contains(sensorId))
                .collect(Collectors.toSet());

        final long dangerCount = criticalSensors.size();
        final long warningCount = warningSensors.size();
        final long normalCount = Math.max(0L, activeSensors - dangerCount - warningCount);

        final Map<Long, String> warehouseNames = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));

        final List<DashboardResponse.LatestReadingSummary> latestReadings = sensors.stream()
                .filter(SensorDevice::isActive)
                .map(sensor -> toLatestReadingSummary(sensor, warehouseNames))
                .filter(Objects::nonNull)
                .toList();

        return new DashboardResponse(totalSensors, activeSensors, normalCount, warningCount, dangerCount,
                latestReadings);
    }

    private DashboardResponse.LatestReadingSummary toLatestReadingSummary(
            final SensorDevice sensor, final Map<Long, String> warehouseNames) {
        final String warehouseName = sensor.getWarehouseId() == null ? null
                : warehouseNames.get(sensor.getWarehouseId());
        return sensorReadingCacheService.latest(sensor.getId())
                .map(reading -> new DashboardResponse.LatestReadingSummary(
                        sensor.getId(),
                        sensor.getName(),
                        sensor.getSensorType(),
                        warehouseName,
                        reading.value(),
                        reading.valueKind(),
                        reading.unit(),
                        reading.status(),
                        reading.recordedAt()))
                .orElse(null);
    }

    /**
     * Returns alerts from the last requested number of days.
     *
     * @param days requested number of days, defaults to 30 when invalid or absent
     * @return newest-first alerts
     */
    @Transactional(readOnly = true)
    public List<SensorAlertResponse> getAlerts(final Integer days) {
        final Map<Long, SensorDevice> sensorMap = sensorDeviceRepository.findAll().stream()
                .collect(Collectors.toMap(SensorDevice::getId, Function.identity()));
        return environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(cutoff(resolveDays(days))).stream()
                .map(alert -> toAlertResponse(alert, sensorMap.get(alert.getSensorDeviceId())))
                .toList();
    }

    /**
     * Acknowledges an environment alert and records the administrator's handling note.
     * Acknowledging clears the alert from the active set (alongside auto-resolution on normalize).
     *
     * @param alertId environment alert id
     * @param note administrator handling/action note (may be blank)
     * @return the updated alert
     */
    @Transactional
    public SensorAlertResponse acknowledgeAlert(final Long alertId, final String note) {
        final EnvironmentAlert alert = environmentAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("환경 알림을 찾을 수 없습니다: " + alertId));

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(currentUserProvider.getCurrentUserEmail());
        alert.setAcknowledgementNote(note);
        final EnvironmentAlert saved = environmentAlertRepository.save(alert);

        final SensorDevice sensor = saved.getSensorDeviceId() == null
                ? null
                : sensorDeviceRepository.findById(saved.getSensorDeviceId()).orElse(null);
        return toAlertResponse(saved, sensor);
    }

    private SensorAlertResponse toAlertResponse(final EnvironmentAlert alert, final SensorDevice sensor) {
        return new SensorAlertResponse(
                alert.getId(),
                alert.getSensorDeviceId(),
                sensor == null ? null : sensor.getName(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.isAcknowledged(),
                alert.getAcknowledgedAt(),
                alert.getAcknowledgedBy(),
                alert.getAcknowledgementNote(),
                alert.getResolvedAt(),
                alert.getCreatedAt());
    }

    private int resolveDays(final Integer requestedDays) {
        if (requestedDays == null || requestedDays <= 0) {
            return DEFAULT_DAYS;
        }
        return requestedDays;
    }

    private Instant cutoff(final int days) {
        return Instant.now().minus(Duration.ofDays(days));
    }
}
