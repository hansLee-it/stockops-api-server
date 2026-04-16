package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.dto.SensorHistoryResponse;
import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.SensorDevice;
import com.stockops.entity.SensorReading;
import com.stockops.entity.SensorType;
import com.stockops.environment.ingestion.SensorLatestProjection;
import com.stockops.environment.ingestion.SensorLatestProjectionRepository;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import com.stockops.repository.SensorReadingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link EnvironmentQueryService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentQueryServiceTest {

    @Mock
    private SensorDeviceRepository sensorDeviceRepository;

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private EnvironmentAlertRepository environmentAlertRepository;

    @Mock
    private SensorLatestProjectionRepository sensorLatestProjectionRepository;

    @InjectMocks
    private EnvironmentQueryService environmentQueryService;

    /**
     * Verifies that the dashboard aggregates counts and sorts latest projections newest first.
     */
    @Test
    void getDashboardAggregatesCountsAndSortsLatestReadings() {
        final SensorDevice activeSensor = sensor(1L, "Temp-1", SensorType.TEMPERATURE, true);
        final SensorDevice inactiveSensor = sensor(2L, "Humidity-1", SensorType.HUMIDITY, false);
        when(sensorDeviceRepository.findAll()).thenReturn(List.of(activeSensor, inactiveSensor));

        final SensorLatestProjection older = projection(2L, 45.0, "humidity", "%", "ok", Instant.parse("2026-04-01T00:00:00Z"), 5L);
        final SensorLatestProjection newer = projection(1L, 3.2, "temperature", "C", "ok", Instant.parse("2026-04-02T00:00:00Z"), 7L);
        when(sensorLatestProjectionRepository.findAll()).thenReturn(List.of(older, newer));

        final EnvironmentAlert infoAlert = alert(10L, 1L, AlertSeverity.INFO, Instant.parse("2026-04-02T00:00:00Z"));
        final EnvironmentAlert warningAlert = alert(11L, 1L, AlertSeverity.WARNING, Instant.parse("2026-04-02T01:00:00Z"));
        final EnvironmentAlert criticalAlert = alert(12L, 2L, AlertSeverity.CRITICAL, Instant.parse("2026-04-02T02:00:00Z"));
        when(environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(any(Instant.class)))
                .thenReturn(List.of(criticalAlert, warningAlert, infoAlert));

        final DashboardResponse response = environmentQueryService.getDashboard();

        assertThat(response.totalSensors()).isEqualTo(2);
        assertThat(response.activeSensors()).isEqualTo(1);
        assertThat(response.normalCount()).isEqualTo(1);
        assertThat(response.warningCount()).isEqualTo(1);
        assertThat(response.dangerCount()).isEqualTo(1);
        assertThat(response.latestReadings())
                .extracting(DashboardResponse.LatestReadingSummary::sensorId)
                .containsExactly(1L, 2L);
        assertThat(response.latestReadings().get(0).sensorName()).isEqualTo("Temp-1");
    }

    /**
     * Verifies that empty repositories produce a zeroed dashboard instead of null collections.
     */
    @Test
    void getDashboardReturnsEmptySummaryWhenNoDataExists() {
        when(sensorDeviceRepository.findAll()).thenReturn(List.of());
        when(sensorLatestProjectionRepository.findAll()).thenReturn(List.of());
        when(environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(any(Instant.class)))
                .thenReturn(List.of());

        final DashboardResponse response = environmentQueryService.getDashboard();

        assertThat(response.totalSensors()).isZero();
        assertThat(response.activeSensors()).isZero();
        assertThat(response.latestReadings()).isEmpty();
    }

    /**
     * Verifies that alert queries default invalid day inputs and tolerate missing sensor metadata.
     */
    @Test
    void getAlertsDefaultsInvalidDaysAndMapsMissingSensorMetadata() {
        when(sensorDeviceRepository.findAll()).thenReturn(List.of(sensor(1L, "Temp-1", SensorType.TEMPERATURE, true)));
        final EnvironmentAlert knownSensorAlert = alert(10L, 1L, AlertSeverity.WARNING, Instant.parse("2026-04-03T00:00:00Z"));
        knownSensorAlert.setMessage("warning");
        final EnvironmentAlert unknownSensorAlert = alert(11L, 999L, AlertSeverity.CRITICAL, Instant.parse("2026-04-03T01:00:00Z"));
        unknownSensorAlert.setMessage("critical");
        when(environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(any(Instant.class)))
                .thenReturn(List.of(unknownSensorAlert, knownSensorAlert));

        final Instant minimumExpectedCutoff = Instant.now().minus(Duration.ofDays(30)).minusSeconds(2);
        final List<SensorAlertResponse> response = environmentQueryService.getAlerts(0);

        final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(environmentAlertRepository).findAllByCreatedAtAfterOrderByCreatedAtDesc(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue())
                .isAfterOrEqualTo(minimumExpectedCutoff)
                .isBeforeOrEqualTo(Instant.now().minus(Duration.ofDays(30)).plusSeconds(2));
        assertThat(response).hasSize(2);
        assertThat(response.get(0).sensorName()).isNull();
        assertThat(response.get(1).sensorName()).isEqualTo("Temp-1");
    }

    /**
     * Verifies that alert queries return an empty list when no recent alerts exist.
     */
    @Test
    void getAlertsReturnsEmptyListWhenNoAlertsExist() {
        when(sensorDeviceRepository.findAll()).thenReturn(List.of());
        when(environmentAlertRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(any(Instant.class)))
                .thenReturn(List.of());

        assertThat(environmentQueryService.getAlerts(null)).isEmpty();
    }

    /**
     * Verifies that history queries forward the resolved cutoff and preserve repository ordering.
     */
    @Test
    void getHistoryMapsReadingsUsingProvidedDayWindow() {
        final SensorReading reading = new SensorReading();
        reading.setSensorDeviceId(5L);
        reading.setValue(12.4);
        reading.setValueKind("temperature");
        reading.setUnit("C");
        reading.setStatus("ok");
        reading.setSequenceId(101L);
        reading.setRecordedAt(Instant.parse("2026-04-04T00:00:00Z"));
        when(sensorReadingRepository.findHistoryBySensorDeviceIdAndRecordedAtAfter(any(Long.class), any(Instant.class)))
                .thenReturn(List.of(reading));

        final List<SensorHistoryResponse> response = environmentQueryService.getHistory(5L, 7);

        final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(sensorReadingRepository)
                .findHistoryBySensorDeviceIdAndRecordedAtAfter(org.mockito.ArgumentMatchers.eq(5L), cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue())
                .isAfterOrEqualTo(Instant.now().minus(Duration.ofDays(7)).minusSeconds(2))
                .isBeforeOrEqualTo(Instant.now().minus(Duration.ofDays(7)).plusSeconds(2));
        assertThat(response).singleElement().satisfies(history -> {
            assertThat(history.sensorId()).isEqualTo(5L);
            assertThat(history.value()).isEqualTo(12.4);
            assertThat(history.sequenceId()).isEqualTo(101L);
        });
    }

    /**
     * Verifies that history queries return an empty list when no readings match the window.
     */
    @Test
    void getHistoryReturnsEmptyListWhenNoReadingsExist() {
        when(sensorReadingRepository.findHistoryBySensorDeviceIdAndRecordedAtAfter(any(Long.class), any(Instant.class)))
                .thenReturn(List.of());

        assertThat(environmentQueryService.getHistory(5L, null)).isEmpty();
    }

    private SensorDevice sensor(final Long id, final String name, final SensorType sensorType, final boolean active) {
        final SensorDevice sensor = new SensorDevice();
        sensor.setId(id);
        sensor.setName(name);
        sensor.setLocation("warehouse-a");
        sensor.setSensorType(sensorType);
        sensor.setActive(active);
        sensor.setDeleted(false);
        return sensor;
    }

    private SensorLatestProjection projection(
            final Long sensorId,
            final Double value,
            final String valueKind,
            final String unit,
            final String status,
            final Instant recordedAt,
            final Long sequenceId) {
        final SensorLatestProjection projection = new SensorLatestProjection();
        projection.setSensorDeviceId(sensorId);
        projection.setValue(value);
        projection.setValueKind(valueKind);
        projection.setUnit(unit);
        projection.setStatus(status);
        projection.setRecordedAt(recordedAt);
        projection.setSequenceId(sequenceId);
        projection.setUpdatedAt(recordedAt);
        return projection;
    }

    private EnvironmentAlert alert(final Long id, final Long sensorId, final AlertSeverity severity, final Instant createdAt) {
        final EnvironmentAlert alert = new EnvironmentAlert();
        alert.setId(id);
        alert.setSensorDeviceId(sensorId);
        alert.setAlertType("threshold");
        alert.setSeverity(severity);
        alert.setMessage("message");
        alert.setAcknowledged(false);
        alert.setCreatedAt(createdAt);
        return alert;
    }
}
