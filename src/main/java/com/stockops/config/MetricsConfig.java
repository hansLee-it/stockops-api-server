package com.stockops.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics configuration for StockOps business metrics.
 * Provides typed helper methods for recording counters and timers
 * used across services to track inventory operations, alerts,
 * notifications, and dashboard performance.
 *
 * @author StockOps Team
 * @since 1.0
 * @see io.micrometer.core.instrument.MeterRegistry
 */
@Component
public class MetricsConfig {

    private static final String METRIC_INVENTORY_OPERATIONS = "inventory.operations";
    private static final String METRIC_ESCALATION_ALERTS_SENT = "escalation.alerts.sent";
    private static final String METRIC_NOTIFICATIONS_SENT = "notifications.sent";
    private static final String METRIC_DASHBOARD_SUMMARY_DURATION = "dashboard.summary.duration";

    private static final String TAG_TYPE = "type";
    private static final String TAG_LEVEL = "level";
    private static final String TAG_CHANNEL = "channel";

    private final MeterRegistry registry;

    public MetricsConfig(final MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Records an inventory operation counter increment.
     *
     * @param type the operation type: inbound, outbound, or adjustment
     */
    public void recordInventoryOperation(final String type) {
        registry.counter(METRIC_INVENTORY_OPERATIONS, TAG_TYPE, type).increment();
    }

    /**
     * Records an escalation alert sent counter increment.
     *
     * @param level the alert severity level: info, warning, critical, or notice
     */
    public void recordEscalationAlert(final String level) {
        registry.counter(METRIC_ESCALATION_ALERTS_SENT, TAG_LEVEL, level.toLowerCase()).increment();
    }

    /**
     * Records a notification sent counter increment.
     *
     * @param channel the notification channel: in_app, email, sms, webhook, or kakao
     */
    public void recordNotificationSent(final String channel) {
        registry.counter(METRIC_NOTIFICATIONS_SENT, TAG_CHANNEL, channel.toLowerCase()).increment();
    }

    /**
     * Starts a timer sample for dashboard summary duration measurement.
     *
     * @return a timer sample that should be stopped with {@link #recordDashboardDuration(Timer.Sample)}
     */
    public Timer.Sample startDashboardTimer() {
        return Timer.start(registry);
    }

    /**
     * Stops a timer sample and records the dashboard summary duration.
     *
     * @param sample the timer sample started by {@link #startDashboardTimer()}
     */
    public void recordDashboardDuration(final Timer.Sample sample) {
        sample.stop(registry.timer(METRIC_DASHBOARD_SUMMARY_DURATION));
    }
}
