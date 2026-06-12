package com.stockops.environment.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.EnvironmentAlertNotification;
import com.stockops.entity.SensorDevice;
import com.stockops.environment.cache.SensorReadingCacheService;
import com.stockops.environment.cache.SensorReadingSnapshot;
import com.stockops.repository.EnvironmentAlertNotificationRepository;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TelemetryIngestionService} event lifecycle.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceTest {

    private static final String TOPIC = "sensimul/sites/site-a/sensors/sensor-01";

    @Mock
    private SensorDeviceRepository sensorDeviceRepository;

    @Mock
    private EnvironmentAlertRepository environmentAlertRepository;

    @Mock
    private EnvironmentAlertNotificationRepository alertNotificationRepository;

    @Mock
    private SensorReadingCacheService sensorReadingCacheService;

    @InjectMocks
    private TelemetryIngestionService telemetryIngestionService;

    /**
     * Stubs the alert repository to assign an id on save (mirrors DB identity generation),
     * which the service needs to create the notification outbox row.
     */
    private void stubAlertSaveAssignsId() {
        when(environmentAlertRepository.save(any(EnvironmentAlert.class))).thenAnswer(invocation -> {
            final EnvironmentAlert alert = invocation.getArgument(0);
            if (alert.getId() == null) {
                alert.setId(123L);
            }
            return alert;
        });
    }

    /**
     * Verifies a WARNING status opens a new active alert when none exists for the sensor.
     */
    @Test
    void ingestOpensWarningAlertWhenNoActiveAlert() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.empty());
        stubAlertSaveAssignsId();

        telemetryIngestionService.ingest(payload("WARNING", "2026-04-05T00:00:00Z"));

        final ArgumentCaptor<EnvironmentAlert> captor = ArgumentCaptor.forClass(EnvironmentAlert.class);
        verify(environmentAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getSensorDeviceId()).isEqualTo(5L);
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(captor.getValue().isAcknowledged()).isFalse();
        assertThat(captor.getValue().getResolvedAt()).isNull();
        final ArgumentCaptor<EnvironmentAlertNotification> outboxCaptor =
                ArgumentCaptor.forClass(EnvironmentAlertNotification.class);
        verify(alertNotificationRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getTriggerType())
                .isEqualTo(EnvironmentAlertNotification.TriggerType.OPENED);
        assertThat(outboxCaptor.getValue().getStatus())
                .isEqualTo(EnvironmentAlertNotification.Status.PENDING);
        assertThat(outboxCaptor.getValue().getSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    /**
     * Verifies an escalation updates the active alert severity from WARNING to CRITICAL.
     */
    @Test
    void ingestEscalatesActiveAlertSeverity() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        final EnvironmentAlert active = activeAlert(5L, AlertSeverity.WARNING);
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.of(active));
        stubAlertSaveAssignsId();

        telemetryIngestionService.ingest(payload("CRITICAL", "2026-04-05T00:00:00Z"));

        final ArgumentCaptor<EnvironmentAlert> captor = ArgumentCaptor.forClass(EnvironmentAlert.class);
        verify(environmentAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        final ArgumentCaptor<EnvironmentAlertNotification> outboxCaptor =
                ArgumentCaptor.forClass(EnvironmentAlertNotification.class);
        verify(alertNotificationRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getTriggerType())
                .isEqualTo(EnvironmentAlertNotification.TriggerType.ESCALATED);
        assertThat(outboxCaptor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    /**
     * Verifies a CRITICAL→WARNING de-escalation updates the alert but does not re-notify.
     */
    @Test
    void ingestDeEscalationDoesNotQueueNotification() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        final EnvironmentAlert active = activeAlert(5L, AlertSeverity.CRITICAL);
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.of(active));

        telemetryIngestionService.ingest(payload("WARNING", "2026-04-05T00:00:00Z"));

        verify(environmentAlertRepository).save(any());
        verify(alertNotificationRepository, never()).save(any());
    }

    /**
     * Verifies a repeated same-severity event does not create or rewrite an alert.
     */
    @Test
    void ingestDoesNotDuplicateActiveAlertOfSameSeverity() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.of(activeAlert(5L, AlertSeverity.WARNING)));

        telemetryIngestionService.ingest(payload("WARNING", "2026-04-05T00:00:00Z"));

        verify(environmentAlertRepository, never()).save(any());
    }

    /**
     * Verifies a normal status auto-resolves the sensor's active alert.
     */
    @Test
    void ingestResolvesActiveAlertOnNormalStatus() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        final EnvironmentAlert active = activeAlert(5L, AlertSeverity.WARNING);
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.of(active));

        telemetryIngestionService.ingest(payload("ok", "2026-04-05T00:00:00Z"));

        final ArgumentCaptor<EnvironmentAlert> captor = ArgumentCaptor.forClass(EnvironmentAlert.class);
        verify(environmentAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        verify(alertNotificationRepository, never()).save(any());
    }

    /**
     * Verifies a normal status with no active alert performs no writes.
     */
    @Test
    void ingestNormalStatusWithoutActiveAlertDoesNothing() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.empty());

        telemetryIngestionService.ingest(payload("ok", "2026-04-05T00:00:00Z"));

        verify(environmentAlertRepository, never()).save(any());
    }

    /**
     * Verifies a configured critical threshold overrides the payload status:
     * an "ok" reading whose value exceeds critMax opens a CRITICAL alert.
     */
    @Test
    void ingestUsesThresholdsOverPayloadStatusWhenConfigured() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        device.setWarnMin(0.0);
        device.setWarnMax(8.0);
        device.setCritMin(-5.0);
        device.setCritMax(10.0);
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.empty());
        stubAlertSaveAssignsId();

        telemetryIngestionService.ingest(payload("ok", "2026-04-05T00:00:00Z")); // value 12.5 > critMax 10

        final ArgumentCaptor<EnvironmentAlert> captor = ArgumentCaptor.forClass(EnvironmentAlert.class);
        verify(environmentAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    /**
     * Verifies an in-bounds value resolves the active alert even when the payload claims WARNING.
     */
    @Test
    void ingestThresholdsResolveAlertForInBoundsValueDespitePayloadStatus() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        device.setWarnMin(0.0);
        device.setWarnMax(20.0);
        final EnvironmentAlert active = activeAlert(5L, AlertSeverity.WARNING);
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.of(active));

        telemetryIngestionService.ingest(payload("WARNING", "2026-04-05T00:00:00Z")); // value 12.5 in [0,20]

        final ArgumentCaptor<EnvironmentAlert> captor = ArgumentCaptor.forClass(EnvironmentAlert.class);
        verify(environmentAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        verify(alertNotificationRepository, never()).save(any());
    }

    /**
     * Verifies a value past the warning bound but inside the critical bound opens a WARNING alert.
     */
    @Test
    void ingestThresholdsOpenWarningBetweenWarnAndCritBounds() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        device.setWarnMax(10.0);
        device.setCritMax(20.0);
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.empty());
        stubAlertSaveAssignsId();

        telemetryIngestionService.ingest(payload("ok", "2026-04-05T00:00:00Z")); // value 12.5 in (10,20]

        final ArgumentCaptor<EnvironmentAlert> captor = ArgumentCaptor.forClass(EnvironmentAlert.class);
        verify(environmentAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    /**
     * Verifies every valid reading is written to the shared recent-reading cache,
     * including the device-unit fallback when the payload omits a unit.
     */
    @Test
    void ingestCachesNormalizedReading() {
        final SensorDevice device = sensorDevice(5L, "Temp-1", "C");
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.of(device));
        when(environmentAlertRepository
                .findFirstBySensorDeviceIdAndResolvedAtIsNullAndAcknowledgedFalseOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.empty());

        telemetryIngestionService.ingest(new SensimulPayload("site-a", "sensor-01", "temperature",
                "temperature", 12.5, null, "ok", "2026-04-05T00:00:00Z", 10L, "1.0"));

        final ArgumentCaptor<SensorReadingSnapshot> captor = ArgumentCaptor.forClass(SensorReadingSnapshot.class);
        verify(sensorReadingCacheService).append(captor.capture());
        final SensorReadingSnapshot snapshot = captor.getValue();
        assertThat(snapshot.sensorDeviceId()).isEqualTo(5L);
        assertThat(snapshot.siteId()).isEqualTo("site-a");
        assertThat(snapshot.sensorId()).isEqualTo("sensor-01");
        assertThat(snapshot.value()).isEqualTo(12.5);
        assertThat(snapshot.unit()).isEqualTo("C");
        assertThat(snapshot.status()).isEqualTo("ok");
        assertThat(snapshot.recordedAt()).isEqualTo(Instant.parse("2026-04-05T00:00:00Z"));
        assertThat(snapshot.sequenceId()).isEqualTo(10L);
    }

    /**
     * Verifies readings for unknown sensors are not cached.
     */
    @Test
    void ingestDoesNotCacheUnknownSensorReading() {
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.empty());

        telemetryIngestionService.ingest(payload("WARNING", "2026-04-05T00:00:00Z"));

        verify(sensorReadingCacheService, never()).append(any());
    }

    /**
     * Verifies malformed payloads are ignored without repository access.
     */
    @Test
    void ingestSkipsMalformedPayload() {
        telemetryIngestionService.ingest(new SensimulPayload(
                "", "sensor-01", "temperature", "temperature", 12.5, "C", "WARNING",
                "2026-04-05T00:00:00Z", 10L, "1.0"));

        verify(sensorDeviceRepository, never()).findByMqttTopic(any());
        verify(environmentAlertRepository, never()).save(any());
    }

    /**
     * Verifies invalid timestamps are dropped before repository access.
     */
    @Test
    void ingestSkipsInvalidTimestamp() {
        telemetryIngestionService.ingest(payload("WARNING", "not-a-timestamp"));

        verify(sensorDeviceRepository, never()).findByMqttTopic(any());
        verify(environmentAlertRepository, never()).save(any());
    }

    /**
     * Verifies telemetry for unknown or deleted sensors is ignored.
     */
    @Test
    void ingestSkipsUnknownSensorTopic() {
        when(sensorDeviceRepository.findByMqttTopic(TOPIC)).thenReturn(Optional.empty());

        telemetryIngestionService.ingest(payload("CRITICAL", "2026-04-05T00:00:00Z"));

        verify(environmentAlertRepository, never()).save(any());
    }

    private SensimulPayload payload(final String status, final String timestamp) {
        return new SensimulPayload("site-a", "sensor-01", "temperature", "temperature", 12.5, "C", status,
                timestamp, 10L, "1.0");
    }

    private SensorDevice sensorDevice(final Long id, final String name, final String unit) {
        final SensorDevice sensorDevice = new SensorDevice();
        sensorDevice.setId(id);
        sensorDevice.setName(name);
        sensorDevice.setMqttTopic(TOPIC);
        sensorDevice.setUnit(unit);
        sensorDevice.setDeleted(false);
        sensorDevice.setActive(true);
        return sensorDevice;
    }

    private EnvironmentAlert activeAlert(final Long sensorDeviceId, final AlertSeverity severity) {
        final EnvironmentAlert alert = new EnvironmentAlert();
        alert.setId(99L);
        alert.setSensorDeviceId(sensorDeviceId);
        alert.setAlertType("temperature");
        alert.setSeverity(severity);
        alert.setMessage("existing");
        alert.setAcknowledged(false);
        return alert;
    }
}
