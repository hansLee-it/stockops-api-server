package com.stockops.service;

import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.dto.SensorHistoryResponse;
import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.SensorDevice;
import com.stockops.entity.SensorReading;
import com.stockops.environment.ingestion.SensorLatestProjection;
import com.stockops.environment.ingestion.SensorLatestProjectionRepository;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import com.stockops.repository.SensorReadingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only environment dashboard, alert, and history query service.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class EnvironmentQueryService {

    private static final int DEFAULT_DAYS = 30;

    private final SensorDeviceRepository sensorDeviceRepository;

    private final SensorReadingRepository sensorReadingRepository;

    private final EnvironmentAlertRepository environmentAlertRepository;

    private final SensorLatestProjectionRepository sensorLatestProjectionRepository;

    /**
     * Creates the service.
     *
     * @param sensorDeviceRepository sensor repository
     * @param sensorReadingRepository sensor reading repository
     * @param environmentAlertRepository environment alert repository
     * @param sensorLatestProjectionRepository latest projection repository
     */
    public EnvironmentQueryService(
            final SensorDeviceRepository sensorDeviceRepository,
            final SensorReadingRepository sensorReadingRepository,
            final EnvironmentAlertRepository environmentAlertRepository,
            final SensorLatestProjectionRepository sensorLatestProjectionRepository) {
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.environmentAlertRepository = environmentAlertRepository;
        this.sensorLatestProjectionRepository = sensorLatestProjectionRepository;
    }

    /**
     * Returns aggregated dashboard data for the environment domain.
     *
     * @return dashboard response
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        final List<SensorDevice> sensors = sensorDeviceRepository.findAll();
        final Map<Long, SensorDevice> sensorMap = sensors.stream()
                .collect(java.util.stream.Collectors.toMap(SensorDevice::getId, Function.identity()));
        final List<SensorLatestProjection> latestProjections = sensorLatestProjectionRepository.findAll();
        final Instant cutoff = cutoff(DEFAULT_DAYS);
        final List<EnvironmentAlert> recentAlerts = environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(cutoff);

        final long totalSensors = sensors.size();
        final long activeSensors = sensors.stream().filter(SensorDevice::isActive).count();
        final long normalCount = recentAlerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.INFO).count();
        final long warningCount = recentAlerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.WARNING).count();
        final long dangerCount = recentAlerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count();

        final List<DashboardResponse.LatestReadingSummary> latestReadings = latestProjections.stream()
                .sorted(Comparator.comparing(SensorLatestProjection::getRecordedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(projection -> toLatestReadingSummary(projection, sensorMap.get(projection.getSensorDeviceId())))
                .toList();

        return new DashboardResponse(totalSensors, activeSensors, normalCount, warningCount, dangerCount, latestReadings);
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
                .collect(java.util.stream.Collectors.toMap(SensorDevice::getId, Function.identity()));
        return environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(cutoff(resolveDays(days))).stream()
                .map(alert -> toAlertResponse(alert, sensorMap.get(alert.getSensorDeviceId())))
                .toList();
    }

    /**
     * Returns time-series history for a sensor from the last requested number of days.
     *
     * @param sensorId sensor device identifier
     * @param days requested number of days, defaults to 30 when invalid or absent
     * @return oldest-first sensor reading history
     */
    @Transactional(readOnly = true)
    public List<SensorHistoryResponse> getHistory(final Long sensorId, final Integer days) {
        return sensorReadingRepository.findHistoryBySensorDeviceIdAndRecordedAtAfter(sensorId, cutoff(resolveDays(days))).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private DashboardResponse.LatestReadingSummary toLatestReadingSummary(
            final SensorLatestProjection projection,
            final SensorDevice sensor) {
        return new DashboardResponse.LatestReadingSummary(
                projection.getSensorDeviceId(),
                sensor == null ? null : sensor.getName(),
                sensor == null ? null : sensor.getSensorType(),
                sensor == null ? null : sensor.getLocation(),
                projection.getValue(),
                projection.getValueKind(),
                projection.getUnit(),
                projection.getStatus(),
                projection.getRecordedAt());
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

    private SensorHistoryResponse toHistoryResponse(final SensorReading reading) {
        return new SensorHistoryResponse(
                reading.getSensorDeviceId(),
                reading.getValue(),
                reading.getValueKind(),
                reading.getUnit(),
                reading.getStatus(),
                reading.getSequenceId(),
                reading.getRecordedAt());
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
