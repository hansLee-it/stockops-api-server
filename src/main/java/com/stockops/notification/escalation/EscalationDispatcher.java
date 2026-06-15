package com.stockops.notification.escalation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Dispatches escalation notifications through configured channels.
 *
 * <p>SMS and email dispatch were removed. Real-time alert delivery now flows through
 * the webhook path ({@code EnvironmentAlertNotifier} → {@code WebhookService}), not the
 * escalation scheduler; remaining escalation channels are logged as placeholders until
 * a delivery integration is wired in.
 *
 * @author StockOps Team
 * @since 2.0
 * @see EscalationScheduler
 */
@Service
public class EscalationDispatcher {

    private static final String CHANNEL_SLACK = "SLACK";

    /**
     * Dispatches an escalation notification through all channels configured in the rule.
     *
     * @param alert the pending alert being escalated
     * @param rule  the escalation rule defining channels and target roles
     */
    public void dispatch(final PendingAlert alert, final EscalationRule rule) {
        for (final String channel : rule.getChannels()) {
            switch (channel.toUpperCase()) {
                case CHANNEL_SLACK -> dispatchSlack(alert, rule);
                default -> log.warn("Unsupported escalation channel '{}' for alert id={}, skipping",
                        channel, alert.getId());
            }
        }
    }

    private void dispatchSlack(final PendingAlert alert, final EscalationRule rule) {
        log.info("[SLACK PLACEHOLDER] Would send Slack notification for alert id={}, level={}, roles={}",
                alert.getId(), rule.getLevel(), rule.getNotifyRoles());
    }

    private static final Logger log = LoggerFactory.getLogger(EscalationDispatcher.class);

}
