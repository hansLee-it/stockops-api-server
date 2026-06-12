package com.stockops.environment.outbox;

import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.EnvironmentAlertNotification;
import com.stockops.entity.SensorDevice;
import com.stockops.environment.EnvironmentAlertNotifier;
import com.stockops.repository.EnvironmentAlertNotificationRepository;
import com.stockops.repository.EnvironmentAlertRepository;
import com.stockops.repository.SensorDeviceRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled outbox sender for environment alert notifications.
 *
 * <p>Claims PENDING rows with {@code FOR UPDATE SKIP LOCKED}, so the sender is safe to run on
 * every load-balanced instance simultaneously — each row is delivered by exactly one claimer.
 * When several pending rows exist for the same alert (open → escalate before the first send),
 * only the newest is delivered and older rows are marked SUPERSEDED. Failed deliveries stay
 * PENDING and retry on later polls until {@code max-attempts}, then become FAILED. Delivery is
 * at-least-once: a partial failure (e.g. email after webhook) re-sends the whole notification.
 *
 * @author StockOps Team
 * @since 2.3
 */
@Service
@ConditionalOnProperty(name = "stockops.environment.alert-outbox.enabled",
        havingValue = "true", matchIfMissing = true)
public class EnvironmentAlertNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentAlertNotificationSender.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final EnvironmentAlertNotificationRepository notificationRepository;
    private final EnvironmentAlertRepository alertRepository;
    private final SensorDeviceRepository sensorDeviceRepository;
    private final EnvironmentAlertNotifier notifier;
    private final AlertOutboxProperties properties;

    /**
     * Creates the sender.
     *
     * @param notificationRepository outbox repository
     * @param alertRepository environment alert repository
     * @param sensorDeviceRepository sensor device repository
     * @param notifier webhook/email dispatch
     * @param properties outbox configuration
     */
    public EnvironmentAlertNotificationSender(
            final EnvironmentAlertNotificationRepository notificationRepository,
            final EnvironmentAlertRepository alertRepository,
            final SensorDeviceRepository sensorDeviceRepository,
            final EnvironmentAlertNotifier notifier,
            final AlertOutboxProperties properties) {
        this.notificationRepository = notificationRepository;
        this.alertRepository = alertRepository;
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.notifier = notifier;
        this.properties = properties;
    }

    /**
     * Claims and delivers a batch of pending notifications.
     */
    @Scheduled(fixedDelayString = "${stockops.environment.alert-outbox.poll-interval:PT15S}")
    @Transactional
    public void deliverPendingNotifications() {
        final List<EnvironmentAlertNotification> claimed =
                notificationRepository.claimPending(PageRequest.of(0, properties.getBatchSize()));
        if (claimed.isEmpty()) {
            return;
        }

        final Map<Long, EnvironmentAlertNotification> newestPerAlert = new HashMap<>();
        for (final EnvironmentAlertNotification notification : claimed) {
            newestPerAlert.merge(notification.getAlertId(), notification,
                    (current, candidate) -> candidate.getId() > current.getId() ? candidate : current);
        }

        for (final EnvironmentAlertNotification notification : claimed) {
            if (newestPerAlert.get(notification.getAlertId()) != notification) {
                notification.setStatus(EnvironmentAlertNotification.Status.SUPERSEDED);
                continue;
            }
            deliver(notification);
        }
    }

    private void deliver(final EnvironmentAlertNotification notification) {
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        try {
            final EnvironmentAlert alert = alertRepository.findById(notification.getAlertId()).orElse(null);
            if (alert == null) {
                notification.setStatus(EnvironmentAlertNotification.Status.FAILED);
                notification.setLastError("alert not found: " + notification.getAlertId());
                return;
            }
            final SensorDevice device = alert.getSensorDeviceId() == null
                    ? null
                    : sensorDeviceRepository.findById(alert.getSensorDeviceId()).orElse(null);
            notifier.dispatch(alert, device);
            notification.setStatus(EnvironmentAlertNotification.Status.SENT);
            notification.setSentAt(Instant.now());
            notification.setLastError(null);
        } catch (final RuntimeException exception) {
            final String message = exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            notification.setLastError(message.length() > MAX_ERROR_LENGTH
                    ? message.substring(0, MAX_ERROR_LENGTH) : message);
            if (notification.getAttemptCount() >= properties.getMaxAttempts()) {
                notification.setStatus(EnvironmentAlertNotification.Status.FAILED);
                LOGGER.error("Alert notification permanently failed after {} attempts (alertId={}): {}",
                        notification.getAttemptCount(), notification.getAlertId(), message);
            } else {
                LOGGER.warn("Alert notification attempt {} failed (alertId={}), will retry: {}",
                        notification.getAttemptCount(), notification.getAlertId(), message);
            }
        }
    }
}
