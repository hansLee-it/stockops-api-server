package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockops.dto.DashboardResponse;
import com.stockops.dto.SensorAlertResponse;
import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.SensorDevice;
import com.stockops.entity.SensorType;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import com.stockops.security.CurrentUserProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    private EnvironmentAlertRepository environmentAlertRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private EnvironmentQueryService environmentQueryService;

    /**
     * Verifies that the dashboard derives normal/warning/danger counts from active alerts
     * and returns no server-side latest readings.
     */
    @Test
    void getDashboardDerivesCountsFromActiveAlerts() {
        final SensorDevice critical = sensor(1L, "Temp-1", SensorType.TEMPERATURE, true);
        final SensorDevice warning = sensor(2L, "Humidity-1", SensorType.HUMIDITY, true);
        final SensorDevice healthy = sensor(3L, "Temp-2", SensorType.TEMPERATURE, true);
        when(sensorDeviceRepository.findAll()).thenReturn(List.of(critical, warning, healthy));

        final EnvironmentAlert criticalAlert = alert(12L, 1L, AlertSeverity.CRITICAL, Instant.parse("2026-04-02T02:00:00Z"));
        final EnvironmentAlert warningAlert = alert(11L, 2L, AlertSeverity.WARNING, Instant.parse("2026-04-02T01:00:00Z"));
        when(environmentAlertRepository.findByResolvedAtIsNullAndAcknowledgedFalse())
                .thenReturn(List.of(criticalAlert, warningAlert));

        final DashboardResponse response = environmentQueryService.getDashboard();

        assertThat(response.totalSensors()).isEqualTo(3);
        assertThat(response.activeSensors()).isEqualTo(3);
        assertThat(response.dangerCount()).isEqualTo(1);
        assertThat(response.warningCount()).isEqualTo(1);
        assertThat(response.normalCount()).isEqualTo(1);
        assertThat(response.latestReadings()).isEmpty();
    }

    /**
     * Verifies that a sensor with both warning and critical active alerts counts once, as danger.
     */
    @Test
    void getDashboardCountsAMixedSensorAsDangerOnly() {
        when(sensorDeviceRepository.findAll())
                .thenReturn(List.of(sensor(1L, "Temp-1", SensorType.TEMPERATURE, true)));
        when(environmentAlertRepository.findByResolvedAtIsNullAndAcknowledgedFalse())
                .thenReturn(List.of(
                        alert(1L, 1L, AlertSeverity.WARNING, Instant.parse("2026-04-02T01:00:00Z")),
                        alert(2L, 1L, AlertSeverity.CRITICAL, Instant.parse("2026-04-02T02:00:00Z"))));

        final DashboardResponse response = environmentQueryService.getDashboard();

        assertThat(response.dangerCount()).isEqualTo(1);
        assertThat(response.warningCount()).isZero();
        assertThat(response.normalCount()).isZero();
    }

    /**
     * Verifies that empty repositories produce a zeroed dashboard instead of null collections.
     */
    @Test
    void getDashboardReturnsEmptySummaryWhenNoDataExists() {
        when(sensorDeviceRepository.findAll()).thenReturn(List.of());
        when(environmentAlertRepository.findByResolvedAtIsNullAndAcknowledgedFalse()).thenReturn(List.of());

        final DashboardResponse response = environmentQueryService.getDashboard();

        assertThat(response.totalSensors()).isZero();
        assertThat(response.activeSensors()).isZero();
        assertThat(response.normalCount()).isZero();
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
     * Verifies that acknowledging an alert records the actor, handling note, and clears active state.
     */
    @Test
    void acknowledgeAlertRecordsHandlingNoteAndActor() {
        final EnvironmentAlert alert = alert(7L, 1L, AlertSeverity.CRITICAL, Instant.parse("2026-04-02T02:00:00Z"));
        when(environmentAlertRepository.findById(7L)).thenReturn(Optional.of(alert));
        when(environmentAlertRepository.save(any(EnvironmentAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProvider.getCurrentUserEmail()).thenReturn("admin@stockops.local");
        when(sensorDeviceRepository.findById(1L))
                .thenReturn(Optional.of(sensor(1L, "Temp-1", SensorType.TEMPERATURE, true)));

        final SensorAlertResponse response = environmentQueryService.acknowledgeAlert(7L, "센서 교체 완료");

        assertThat(response.acknowledged()).isTrue();
        assertThat(response.acknowledgedAt()).isNotNull();
        assertThat(response.acknowledgedBy()).isEqualTo("admin@stockops.local");
        assertThat(response.acknowledgementNote()).isEqualTo("센서 교체 완료");
        assertThat(response.active()).isFalse();
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
