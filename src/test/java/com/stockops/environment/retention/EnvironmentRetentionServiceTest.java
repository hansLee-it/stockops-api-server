package com.stockops.environment.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.repository.EnvironmentAlertNotificationRepository;
import com.stockops.repository.EnvironmentAlertRepository;
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
    private EnvironmentAlertRepository environmentAlertRepository;

    @Mock
    private EnvironmentAlertNotificationRepository alertNotificationRepository;

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
                environmentAlertRepository,
                alertNotificationRepository,
                retentionProperties);
    }

    /**
     * Verifies that old alerts are purged using a UTC cutoff derived from retention days.
     */
    @Test
    void purgeOldAlertsUsesThirtyDayUtcCutoff() {
        when(environmentAlertRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(5);
        final Instant minimumExpectedCutoff = Instant.now().minus(Duration.ofDays(30)).minusSeconds(2);

        final int deletedCount = environmentRetentionService.purgeOldAlerts();

        final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(environmentAlertRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());
        assertThat(deletedCount).isEqualTo(5);
        assertThat(cutoffCaptor.getValue())
                .isAfterOrEqualTo(minimumExpectedCutoff)
                .isBeforeOrEqualTo(Instant.now().minus(Duration.ofDays(30)).plusSeconds(2));
    }

    /**
     * Verifies that a combined purge deletes alerts and reports zero readings (no longer stored).
     */
    @Test
    void purgeAllReturnsAlertCountsAndZeroReadings() {
        when(environmentAlertRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(2);
        when(alertNotificationRepository.deleteTerminalBefore(any(Instant.class))).thenReturn(3);

        final PurgeResult result = environmentRetentionService.purgeAll();

        final ArgumentCaptor<Instant> alertCutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(environmentAlertRepository).deleteByCreatedAtBefore(alertCutoffCaptor.capture());
        verify(alertNotificationRepository).deleteTerminalBefore(any(Instant.class));
        assertThat(result.readingsDeleted()).isZero();
        assertThat(result.alertsDeleted()).isEqualTo(5);
        assertThat(result.cutoffDate()).isEqualTo(alertCutoffCaptor.getValue());
        assertThat(result.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
    }
}
