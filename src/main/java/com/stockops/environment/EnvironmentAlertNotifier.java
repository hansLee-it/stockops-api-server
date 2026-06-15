package com.stockops.environment;

import com.stockops.entity.AlertSeverity;
import com.stockops.entity.EnvironmentAlert;
import com.stockops.entity.SensorDevice;
import com.stockops.notification.webhook.WebhookEndpointConfig;
import com.stockops.notification.webhook.WebhookEndpointConfigRepository;
import com.stockops.notification.webhook.WebhookPayload;
import com.stockops.notification.webhook.WebhookService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delivers environment alert notifications via webhook.
 *
 * <p>Called by the outbox sender, not by telemetry ingestion: ingestion only records
 * notification rows, and the scheduled {@code EnvironmentAlertNotificationSender} claims and
 * delivers them. Failures propagate to the sender so the outbox can retry.
 *
 * @author StockOps Team
 * @since 2.2
 */
@Service
public class EnvironmentAlertNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentAlertNotifier.class);
    private static final String EVENT_TYPE = "ENVIRONMENT_ALERT";

    private final WebhookService webhookService;
    private final WebhookEndpointConfigRepository webhookEndpointConfigRepository;

    /**
     * Creates the notifier.
     *
     * @param webhookService webhook dispatch service
     * @param webhookEndpointConfigRepository enabled webhook endpoint lookup
     */
    public EnvironmentAlertNotifier(
            final WebhookService webhookService,
            final WebhookEndpointConfigRepository webhookEndpointConfigRepository) {
        this.webhookService = webhookService;
        this.webhookEndpointConfigRepository = webhookEndpointConfigRepository;
    }

    /**
     * Delivers the alert to all configured webhook endpoints. Failures propagate so the outbox
     * sender can record the attempt and retry later (delivery is at-least-once: a partial
     * failure re-sends the whole notification on the next attempt).
     *
     * @param alert the opened/escalated alert
     * @param device the related sensor device (may be null)
     */
    public void dispatch(final EnvironmentAlert alert, final SensorDevice device) {
        LOGGER.debug("Dispatching environment alert notification for alertId={}", alert.getId());
        dispatchWebhooks(alert, device);
    }

    private void dispatchWebhooks(final EnvironmentAlert alert, final SensorDevice device) {
        final List<WebhookEndpointConfig> endpoints = webhookEndpointConfigRepository.findByEnabledTrue();
        if (endpoints.isEmpty()) {
            return;
        }
        final WebhookPayload payload = WebhookPayload.builder()
                .eventType(EVENT_TYPE)
                .message(alert.getMessage())
                .severity(toWebhookSeverity(alert.getSeverity()))
                .location(device == null ? null : device.getLocation())
                .alertType(alert.getAlertType())
                .timestamp(Instant.now())
                .build();
        for (final WebhookEndpointConfig endpoint : endpoints) {
            webhookService.send(endpoint.getProviderType().name(), endpoint.getWebhookUrl(), payload);
        }
    }

    private WebhookPayload.Severity toWebhookSeverity(final AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> WebhookPayload.Severity.CRITICAL;
            case WARNING -> WebhookPayload.Severity.WARNING;
            default -> WebhookPayload.Severity.INFO;
        };
    }
}
