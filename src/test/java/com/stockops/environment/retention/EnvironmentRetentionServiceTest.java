package com.stockops.environment.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorReadingRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link EnvironmentRetentionService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentRetentionServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private EnvironmentAlertRepository environmentAlertRepository;

    private EnvironmentRetentionService environmentRetentionService;

    /**
     * Creates a retention service with default 30-day retention for each test.
     */
    @BeforeEach
    void setUp() {
        final RetentionProperties retentionProperties = new RetentionProperties();
        retentionProperties.setRetentionDays(30);
        retentionProperties.setBatchSize(1000);
        retentionProperties.setEnabled(true);
        environmentRetentionService = new EnvironmentRetentionService(
                sensorReadingRepository,
                environmentAlertRepository,
                retentionProperties);
    }

    /**
     * Verifies that old readings are purged using a UTC cutoff derived from retention days.
     */
    @Test
    void purgeOldReadingsUsesThirtyDayUtcCutoff() {
        when(sensorReadingRepository.deleteByRecordedAtBefore(any(Instant.class))).thenReturn(7);
        final Instant minimumExpectedCutoff = Instant.now().minus(Duration.ofDays(30)).minusSeconds(2);

        final int deletedCount = environmentRetentionService.purgeOldReadings();

        final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(sensorReadingRepository).deleteByRecordedAtBefore(cutoffCaptor.capture());
        assertThat(deletedCount).isEqualTo(7);
        assertThat(cutoffCaptor.getValue())
                .isAfterOrEqualTo(minimumExpectedCutoff)
                .isBeforeOrEqualTo(Instant.now().minus(Duration.ofDays(30)).plusSeconds(2));
    }

    /**
     * Verifies that a combined purge deletes both datasets with one shared cutoff summary.
     */
    @Test
    void purgeAllReturnsCombinedCountsAndCutoff() {
        when(sensorReadingRepository.deleteByRecordedAtBefore(any(Instant.class))).thenReturn(4);
        when(environmentAlertRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(2);

        final PurgeResult result = environmentRetentionService.purgeAll();

        final ArgumentCaptor<Instant> readingCutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        final ArgumentCaptor<Instant> alertCutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(sensorReadingRepository).deleteByRecordedAtBefore(readingCutoffCaptor.capture());
        verify(environmentAlertRepository).deleteByCreatedAtBefore(alertCutoffCaptor.capture());
        assertThat(result.readingsDeleted()).isEqualTo(4);
        assertThat(result.alertsDeleted()).isEqualTo(2);
        assertThat(result.cutoffDate()).isEqualTo(readingCutoffCaptor.getValue());
        assertThat(result.cutoffDate()).isEqualTo(alertCutoffCaptor.getValue());
        assertThat(result.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
    }
}
