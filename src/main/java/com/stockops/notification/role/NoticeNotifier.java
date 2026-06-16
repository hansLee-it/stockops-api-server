package com.stockops.notification.role;

import com.stockops.entity.Notice;
import com.stockops.notification.webhook.WebhookPayload;
import com.stockops.notification.webhook.WebhookService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Routes a notice/announcement to the webhook channels mapped to its target roles.
 *
 * <p>If the notice has no target roles it is treated as a broadcast and goes to every enabled
 * role channel. Channels are de-duplicated by URL. Send failures are swallowed by
 * {@link WebhookService} so notice creation is never rolled back by a delivery error.
 *
 * @author StockOps Team
 * @since 2.3
 */
@Service
public class NoticeNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoticeNotifier.class);
    private static final String EVENT_TYPE = "NOTICE";

    private final RoleWebhookConfigRepository roleWebhookConfigRepository;
    private final WebhookService webhookService;

    public NoticeNotifier(final RoleWebhookConfigRepository roleWebhookConfigRepository,
            final WebhookService webhookService) {
        this.roleWebhookConfigRepository = roleWebhookConfigRepository;
        this.webhookService = webhookService;
    }

    /**
     * Delivers the notice to its target-role channels (or all role channels when no target is set).
     *
     * @param notice the persisted notice
     */
    public void dispatch(final Notice notice) {
        final List<String> targetRoles = notice.getTargetRoles();
        final List<RoleWebhookConfig> configs = (targetRoles == null || targetRoles.isEmpty())
                ? roleWebhookConfigRepository.findByEnabledTrue()
                : roleWebhookConfigRepository.findByRoleInAndEnabledTrue(targetRoles);
        if (configs.isEmpty()) {
            LOGGER.debug("No role webhook channel for notice id={} (targetRoles={}); nothing dispatched",
                    notice.getId(), targetRoles);
            return;
        }

        final WebhookPayload payload = WebhookPayload.builder()
                .eventType(EVENT_TYPE)
                .message(buildMessage(notice))
                .severity(WebhookPayload.Severity.INFO)
                .alertType(notice.getType() == null ? null : notice.getType().name())
                .timestamp(Instant.now())
                .build();

        final Set<String> sentUrls = new HashSet<>();
        for (final RoleWebhookConfig config : configs) {
            if (sentUrls.add(config.getWebhookUrl())) {
                webhookService.send(config.getProviderType().name(), config.getWebhookUrl(), payload);
            }
        }
    }

    private String buildMessage(final Notice notice) {
        final String content = notice.getContent();
        return content == null || content.isBlank()
                ? notice.getTitle()
                : notice.getTitle() + "\n\n" + content;
    }
}
