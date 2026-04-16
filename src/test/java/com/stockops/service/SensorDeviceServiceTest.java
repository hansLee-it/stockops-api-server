package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.dto.SensorDeviceRequest;
import com.stockops.dto.SensorDeviceResponse;
import com.stockops.entity.SensorDevice;
import com.stockops.entity.SensorType;
import com.stockops.exception.ConflictException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.SensorDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for {@link SensorDeviceService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SensorDeviceServiceTest {

    @Mock
    private SensorDeviceRepository sensorDeviceRepository;

    @InjectMocks
    private SensorDeviceService sensorDeviceService;

    /**
     * Verifies that a brand-new sensor device is created with the canonical topic.
     */
    @Test
    void createSensorDeviceCreatesNewActiveSensor() {
        final SensorDeviceRequest request = request("site-a", "sensor-01", "warehouse-a");
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(request.mqttTopic())).thenReturn(Optional.empty());
        when(sensorDeviceRepository.findByMqttTopic(request.mqttTopic())).thenReturn(Optional.empty());
        when(sensorDeviceRepository.save(any(SensorDevice.class))).thenAnswer(invocation -> {
            final SensorDevice saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setCreatedAt(Instant.parse("2026-04-10T00:00:00Z"));
            saved.setUpdatedAt(Instant.parse("2026-04-10T01:00:00Z"));
            return saved;
        });

        final SensorDeviceResponse response = sensorDeviceService.createSensorDevice(request);

        final ArgumentCaptor<SensorDevice> captor = ArgumentCaptor.forClass(SensorDevice.class);
        verify(sensorDeviceRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getExternalSensorId()).isEqualTo("sensor-01");
        assertThat(captor.getValue().getMqttTopic()).isEqualTo(request.mqttTopic());
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.siteId()).isEqualTo("site-a");
        assertThat(response.sensorId()).isEqualTo("sensor-01");
        assertThat(response.deleted()).isFalse();
    }

    /**
     * Verifies that create reactivates a matching soft-deleted sensor instead of inserting a duplicate.
     */
    @Test
    void createSensorDeviceReactivatesSoftDeletedSensor() {
        final SensorDeviceRequest request = request("site-a", "sensor-01", "warehouse-b");
        final SensorDevice deletedSensor = sensorDevice(7L, request.mqttTopic(), true, false);
        deletedSensor.setLocation("old-location");

        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(request.mqttTopic())).thenReturn(Optional.empty());
        when(sensorDeviceRepository.findByMqttTopic(request.mqttTopic())).thenReturn(Optional.of(deletedSensor));
        when(sensorDeviceRepository.save(any(SensorDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final SensorDeviceResponse response = sensorDeviceService.createSensorDevice(request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.location()).isEqualTo("warehouse-b");
        assertThat(response.active()).isTrue();
        assertThat(response.deleted()).isFalse();
    }

    /**
     * Verifies that duplicate active topics are rejected during create.
     */
    @Test
    void createSensorDeviceRejectsDuplicateActiveTopic() {
        final SensorDeviceRequest request = request("site-a", "sensor-01", "warehouse-a");
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(request.mqttTopic()))
                .thenReturn(Optional.of(sensorDevice(1L, request.mqttTopic(), false, true)));

        assertThrows(ConflictException.class, () -> sensorDeviceService.createSensorDevice(request));
    }

    /**
     * Verifies that a non-canonical MQTT topic is rejected before repository access.
     */
    @Test
    void createSensorDeviceRejectsTopicMismatch() {
        final SensorDeviceRequest request = new SensorDeviceRequest(
                "site-a",
                "sensor-01",
                SensorType.TEMPERATURE,
                "warehouse-a",
                "sensimul/sites/site-a/sensors/other-sensor",
                "site-a");

        assertThrows(InvalidOperationException.class, () -> sensorDeviceService.createSensorDevice(request));
    }

    /**
     * Verifies that active lookup uses the soft-delete aware repository method.
     */
    @Test
    void getSensorDeviceByIdThrowsWhenActiveRecordMissing() {
        when(sensorDeviceRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sensorDeviceService.getSensorDeviceById(99L));
    }

    /**
     * Verifies that updates reject collisions with another active sensor using the same topic.
     */
    @Test
    void updateSensorDeviceRejectsDuplicateActiveTopicFromDifferentSensor() {
        final SensorDeviceRequest request = request("site-a", "sensor-01", "warehouse-a");
        final SensorDevice current = sensorDevice(10L, "sensimul/sites/site-a/sensors/current", false, true);
        final SensorDevice duplicate = sensorDevice(11L, request.mqttTopic(), false, true);

        when(sensorDeviceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(current));
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(request.mqttTopic())).thenReturn(Optional.of(duplicate));

        assertThrows(ConflictException.class, () -> sensorDeviceService.updateSensorDevice(10L, request));
    }

    /**
     * Verifies that updates rewrite canonical topic-backed fields for the active sensor.
     */
    @Test
    void updateSensorDeviceUpdatesActiveSensor() {
        final SensorDeviceRequest request = request("site-a", "sensor-02", "warehouse-b");
        final SensorDevice current = sensorDevice(10L, "sensimul/sites/site-a/sensors/sensor-01", false, true);
        when(sensorDeviceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(current));
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(request.mqttTopic())).thenReturn(Optional.empty());
        when(sensorDeviceRepository.save(any(SensorDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final SensorDeviceResponse response = sensorDeviceService.updateSensorDevice(10L, request);

        assertThat(response.sensorId()).isEqualTo("sensor-02");
        assertThat(response.location()).isEqualTo("warehouse-b");
        assertThat(response.mqttTopic()).isEqualTo(request.mqttTopic());
    }

    /**
     * Verifies that delete marks the sensor inactive and deleted.
     */
    @Test
    void deleteSensorDeviceSoftDeletesActiveSensor() {
        final SensorDevice sensor = sensorDevice(10L, "sensimul/sites/site-a/sensors/sensor-01", false, true);
        when(sensorDeviceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(sensor));

        sensorDeviceService.deleteSensorDevice(10L);

        final ArgumentCaptor<SensorDevice> captor = ArgumentCaptor.forClass(SensorDevice.class);
        verify(sensorDeviceRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().isActive()).isFalse();
    }

    /**
     * Verifies that reactivation loads from the full repository and restores active flags.
     */
    @Test
    void reactivateSensorDeviceRestoresDeletedSensor() {
        final SensorDevice deletedSensor = sensorDevice(10L, "sensimul/sites/site-a/sensors/sensor-01", true, false);
        when(sensorDeviceRepository.findById(10L)).thenReturn(Optional.of(deletedSensor));
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(deletedSensor.getMqttTopic())).thenReturn(Optional.empty());
        when(sensorDeviceRepository.save(any(SensorDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final SensorDeviceResponse response = sensorDeviceService.reactivateSensorDevice(10L);

        assertThat(response.active()).isTrue();
        assertThat(response.deleted()).isFalse();
    }

    /**
     * Verifies that reactivation fails when another active sensor already owns the topic.
     */
    @Test
    void reactivateSensorDeviceRejectsDuplicateActiveTopic() {
        final SensorDevice deletedSensor = sensorDevice(10L, "sensimul/sites/site-a/sensors/sensor-01", true, false);
        final SensorDevice activeSensor = sensorDevice(11L, deletedSensor.getMqttTopic(), false, true);
        when(sensorDeviceRepository.findById(10L)).thenReturn(Optional.of(deletedSensor));
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(deletedSensor.getMqttTopic()))
                .thenReturn(Optional.of(activeSensor));

        assertThrows(ConflictException.class, () -> sensorDeviceService.reactivateSensorDevice(10L));
    }

    /**
     * Verifies that external id lookup rejects missing active sensors.
     */
    @Test
    void getSensorDeviceByExternalIdsThrowsWhenMissing() {
        when(sensorDeviceRepository.findByMqttTopicAndDeletedFalse("sensimul/sites/site-a/sensors/sensor-01"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sensorDeviceService.getSensorDeviceByExternalIds("site-a", "sensor-01"));
    }

    /**
     * Verifies that paged active sensor queries return an empty page when nothing is registered.
     */
    @Test
    void getSensorDevicesReturnsEmptyPageWhenNoActiveSensorsExist() {
        final PageRequest pageable = PageRequest.of(0, 5);
        when(sensorDeviceRepository.findAllByDeletedFalse(pageable)).thenReturn(Page.empty(pageable));

        final Page<SensorDeviceResponse> response = sensorDeviceService.getSensorDevices(pageable);

        assertThat(response).isEmpty();
    }

    /**
     * Verifies that reactivation rejects missing records loaded from the full repository.
     */
    @Test
    void reactivateSensorDeviceRejectsMissingRecord() {
        when(sensorDeviceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sensorDeviceService.reactivateSensorDevice(10L));
    }

    /**
     * Verifies that reactivation rejects records that are already active.
     */
    @Test
    void reactivateSensorDeviceRejectsAlreadyActiveRecord() {
        when(sensorDeviceRepository.findById(10L))
                .thenReturn(Optional.of(sensorDevice(10L, "sensimul/sites/site-a/sensors/sensor-01", false, true)));

        assertThrows(ResourceNotFoundException.class, () -> sensorDeviceService.reactivateSensorDevice(10L));
    }

    /**
     * Verifies that null requests fail fast rather than silently persisting broken data.
     */
    @Test
    void createSensorDeviceRejectsNullRequest() {
        assertThrows(NullPointerException.class, () -> sensorDeviceService.createSensorDevice(null));
    }

    private SensorDeviceRequest request(final String siteId, final String sensorId, final String location) {
        return new SensorDeviceRequest(
                siteId,
                sensorId,
                SensorType.TEMPERATURE,
                location,
                "sensimul/sites/" + siteId + "/sensors/" + sensorId,
                siteId);
    }

    private SensorDevice sensorDevice(final Long id, final String mqttTopic, final boolean deleted, final boolean active) {
        final SensorDevice sensorDevice = new SensorDevice();
        sensorDevice.setId(id);
        sensorDevice.setName("sensor-name");
        sensorDevice.setLocation("warehouse-a");
        sensorDevice.setSensorType(SensorType.TEMPERATURE);
        sensorDevice.setExternalSensorId("sensor-01");
        sensorDevice.setSourceChannel("site-a");
        sensorDevice.setMqttTopic(mqttTopic);
        sensorDevice.setDeleted(deleted);
        sensorDevice.setActive(active);
        return sensorDevice;
    }
}
