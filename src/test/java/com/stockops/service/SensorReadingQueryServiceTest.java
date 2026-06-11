package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.dto.RecentSensorReadingsResponse;
import com.stockops.entity.SensorDevice;
import com.stockops.entity.SensorType;
import com.stockops.environment.cache.SensorCacheProperties;
import com.stockops.environment.cache.SensorReadingCacheService;
import com.stockops.environment.cache.SensorReadingSnapshot;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.SensorDeviceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SensorReadingQueryService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SensorReadingQueryServiceTest {

    @Mock
    private SensorDeviceRepository sensorDeviceRepository;

    @Mock
    private SensorReadingCacheService sensorReadingCacheService;

    private SensorReadingQueryService sensorReadingQueryService;

    @BeforeEach
    void setUp() {
        sensorReadingQueryService = new SensorReadingQueryService(
                sensorDeviceRepository, sensorReadingCacheService, new SensorCacheProperties());
    }

    /**
     * Verifies cached readings are returned for an existing sensor, oldest first.
     */
    @Test
    void getRecentReadingsMapsCachedSnapshots() {
        when(sensorDeviceRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(sensor(12L)));
        when(sensorReadingCacheService.readRecent(12L, Duration.ofMinutes(10)))
                .thenReturn(List.of(new SensorReadingSnapshot(12L, "site-a", "temp-001", "TEMPERATURE",
                        "temperature", 23.4, "C", "NORMAL", Instant.parse("2026-06-11T09:15:30Z"), 184L)));

        final RecentSensorReadingsResponse response = sensorReadingQueryService.getRecentReadings(12L, 10);

        assertThat(response.sensorId()).isEqualTo(12L);
        assertThat(response.windowMinutes()).isEqualTo(10);
        assertThat(response.readings()).hasSize(1);
        assertThat(response.readings().get(0).value()).isEqualTo(23.4);
        assertThat(response.readings().get(0).status()).isEqualTo("NORMAL");
        assertThat(response.readings().get(0).recordedAt()).isEqualTo(Instant.parse("2026-06-11T09:15:30Z"));
        assertThat(response.readings().get(0).sequenceId()).isEqualTo(184L);
    }

    /**
     * Verifies the requested window is capped at the configured retention window.
     */
    @Test
    void getRecentReadingsCapsRequestedWindow() {
        when(sensorDeviceRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(sensor(12L)));
        when(sensorReadingCacheService.readRecent(12L, Duration.ofMinutes(10))).thenReturn(List.of());

        final RecentSensorReadingsResponse response = sensorReadingQueryService.getRecentReadings(12L, 120);

        assertThat(response.windowMinutes()).isEqualTo(10);
        verify(sensorReadingCacheService).readRecent(12L, Duration.ofMinutes(10));
    }

    /**
     * Verifies a missing minutes parameter defaults to the full retention window.
     */
    @Test
    void getRecentReadingsDefaultsWindowWhenMinutesAbsent() {
        when(sensorDeviceRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(sensor(12L)));
        when(sensorReadingCacheService.readRecent(12L, Duration.ofMinutes(10))).thenReturn(List.of());

        final RecentSensorReadingsResponse response = sensorReadingQueryService.getRecentReadings(12L, null);

        assertThat(response.windowMinutes()).isEqualTo(10);
        assertThat(response.readings()).isEmpty();
    }

    /**
     * Verifies unknown or deleted sensors raise a not-found error.
     */
    @Test
    void getRecentReadingsRejectsUnknownSensor() {
        when(sensorDeviceRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorReadingQueryService.getRecentReadings(99L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private SensorDevice sensor(final Long id) {
        final SensorDevice sensor = new SensorDevice();
        sensor.setId(id);
        sensor.setName("Temp-1");
        sensor.setLocation("warehouse-a");
        sensor.setSensorType(SensorType.TEMPERATURE);
        sensor.setActive(true);
        sensor.setDeleted(false);
        return sensor;
    }
}
