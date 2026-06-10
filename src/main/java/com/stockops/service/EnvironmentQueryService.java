package com.stockops.service;

import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.SensorDevice;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only environment dashboard and alert query service.
 *
 * <p>Live sensor measurements are no longer stored server-side; the dashboard's
 * normal/warning/danger view is derived from the currently <em>active</em> environment alerts
 * (unresolved and unacknowledged events).
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class EnvironmentQueryService {

    private static final int DEFAULT_DAYS = 30;

    private final SensorDeviceRepository sensorDeviceRepository;

    private final EnvironmentAlertRepository environmentAlertRepository;

    /**
     * Creates the service.
     *
     * @param sensorDeviceRepository sensor repository
     * @param environmentAlertRepository environment alert (event) repository
     */
    public EnvironmentQueryService(
            final SensorDeviceRepository sensorDeviceRepository,
            final EnvironmentAlertRepository environmentAlertRepository) {
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.environmentAlertRepository = environmentAlertRepository;
    }

    /**
     * Returns aggregated dashboard data for the environment domain.
     *
     * <p>Latest readings are intentionally empty — real-time values are viewed client-side via MQTT.
     * Normal/warning/danger counts are computed from active alerts: a sensor with an active CRITICAL
     * alert is dangerous, an active WARNING alert is a warning, and the remainder of active sensors
     * are normal.
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

        return new DashboardResponse(totalSensors, activeSensors, normalCount, warningCount, dangerCount, List.of());
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
