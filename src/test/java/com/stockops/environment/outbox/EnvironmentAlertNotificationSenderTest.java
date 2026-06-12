package com.stockops.environment.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.EnvironmentAlertNotification;
import com.stockops.entity.SensorDevice;
import com.stockops.environment.EnvironmentAlertNotifier;
import com.stockops.repository.EnvironmentAlertNotificationRepository;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link EnvironmentAlertNotificationSender} delivery semantics.
 *
 * @author StockOps Team
 * @since 2.3
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentAlertNotificationSenderTest {

    @Mock
    private EnvironmentAlertNotificationRepository notificationRepository;

    @Mock
    private EnvironmentAlertRepository alertRepository;

    @Mock
    private SensorDeviceRepository sensorDeviceRepository;

    @Mock
    private EnvironmentAlertNotifier notifier;

    private EnvironmentAlertNotificationSender sender;

    @BeforeEach
    void setUp() {
        final AlertOutboxProperties properties = new AlertOutboxProperties();
        properties.setMaxAttempts(3);
        sender = new EnvironmentAlertNotificationSender(
                notificationRepository, alertRepository, sensorDeviceRepository, notifier, properties);
    }

    /**
     * Verifies a claimed pending row is delivered and marked SENT.
     */
    @Test
    void deliversClaimedNotificationAndMarksSent() {
        final EnvironmentAlertNotification notification = pendingNotification(1L, 7L);
        when(notificationRepository.claimPending(any(Pageable.class))).thenReturn(List.of(notification));
        when(alertRepository.findById(7L)).thenReturn(Optional.of(alert(7L, 5L)));
        when(sensorDeviceRepository.findById(5L)).thenReturn(Optional.of(new SensorDevice()));

        sender.deliverPendingNotifications();

        verify(notifier).dispatch(any(EnvironmentAlert.class), any(SensorDevice.class));
        assertThat(notification.getStatus()).isEqualTo(EnvironmentAlertNotification.Status.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getAttemptCount()).isEqualTo(1);
    }

    /**
     * Verifies a delivery failure keeps the row pending for retry and records the error.
     */
    @Test
    void failedDeliveryStaysPendingWithError() {
        final EnvironmentAlertNotification notification = pendingNotification(1L, 7L);
        when(notificationRepository.claimPending(any(Pageable.class))).thenReturn(List.of(notification));
        when(alertRepository.findById(7L)).thenReturn(Optional.of(alert(7L, 5L)));
        doThrow(new IllegalStateException("webhook down")).when(notifier).dispatch(any(), any());

        sender.deliverPendingNotifications();

        assertThat(notification.getStatus()).isEqualTo(EnvironmentAlertNotification.Status.PENDING);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getLastError()).contains("webhook down");
    }

    /**
     * Verifies the row becomes FAILED once the attempt limit is reached.
     */
    @Test
    void exhaustedAttemptsMarkNotificationFailed() {
        final EnvironmentAlertNotification notification = pendingNotification(1L, 7L);
        notification.setAttemptCount(2);
        when(notificationRepository.claimPending(any(Pageable.class))).thenReturn(List.of(notification));
        when(alertRepository.findById(7L)).thenReturn(Optional.of(alert(7L, 5L)));
        doThrow(new IllegalStateException("still down")).when(notifier).dispatch(any(), any());

        sender.deliverPendingNotifications();

        assertThat(notification.getStatus()).isEqualTo(EnvironmentAlertNotification.Status.FAILED);
        assertThat(notification.getAttemptCount()).isEqualTo(3);
    }

    /**
     * Verifies that when open and escalate rows are both pending for one alert,
     * only the newest is delivered and the older one is superseded.
     */
    @Test
    void collapsesOlderPendingRowsForSameAlert() {
        final EnvironmentAlertNotification opened = pendingNotification(1L, 7L);
        final EnvironmentAlertNotification escalated = pendingNotification(2L, 7L);
        escalated.setSeverity(AlertSeverity.CRITICAL);
        when(notificationRepository.claimPending(any(Pageable.class))).thenReturn(List.of(opened, escalated));
        when(alertRepository.findById(7L)).thenReturn(Optional.of(alert(7L, 5L)));

        sender.deliverPendingNotifications();

        assertThat(opened.getStatus()).isEqualTo(EnvironmentAlertNotification.Status.SUPERSEDED);
        assertThat(escalated.getStatus()).isEqualTo(EnvironmentAlertNotification.Status.SENT);
        verify(notifier).dispatch(any(), any());
    }

    /**
     * Verifies a row referencing a purged alert fails permanently instead of retrying forever.
     */
    @Test
    void missingAlertFailsImmediately() {
        final EnvironmentAlertNotification notification = pendingNotification(1L, 404L);
        when(notificationRepository.claimPending(any(Pageable.class))).thenReturn(List.of(notification));
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        sender.deliverPendingNotifications();

        assertThat(notification.getStatus()).isEqualTo(EnvironmentAlertNotification.Status.FAILED);
        verify(notifier, never()).dispatch(any(), any());
    }

    private EnvironmentAlertNotification pendingNotification(final Long id, final Long alertId) {
        final EnvironmentAlertNotification notification = EnvironmentAlertNotification.pending(
                alertId, EnvironmentAlertNotification.TriggerType.OPENED, AlertSeverity.WARNING);
        notification.setId(id);
        return notification;
    }

    private EnvironmentAlert alert(final Long id, final Long sensorDeviceId) {
        final EnvironmentAlert alert = new EnvironmentAlert();
        alert.setId(id);
        alert.setSensorDeviceId(sensorDeviceId);
        alert.setSeverity(AlertSeverity.WARNING);
        alert.setMessage("temp high");
        return alert;
    }
}
