package com.stockops.service.ai;

import com.stockops.entity.ai.ForecastProposalRun;
import com.stockops.notification.role.RoleWebhookConfig;
import com.stockops.notification.role.RoleWebhookConfigRepository;
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
 * Pushes a single per-slot summary of fresh intraday proposals to the role-mapped webhook channels
 * (Teams). One summary message per slot keeps the channel readable instead of one ping per SKU.
 *
 * <p>Routing/dedup mirrors {@link com.stockops.notification.role.NoticeNotifier}. Delivery failures
 * are swallowed by {@link WebhookService} so a webhook problem never rolls back a forecast run.
 *
 * @author StockOps Team
 * @since 2.4
 */
@Service
public class IntradayProposalNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntradayProposalNotifier.class);
    private static final String EVENT_TYPE = "AI_INTRADAY_PROPOSAL";

    private final IntradayForecastProperties properties;
    private final RoleWebhookConfigRepository roleWebhookConfigRepository;
    private final WebhookService webhookService;

    public IntradayProposalNotifier(final IntradayForecastProperties properties,
            final RoleWebhookConfigRepository roleWebhookConfigRepository,
            final WebhookService webhookService) {
        this.properties = properties;
        this.roleWebhookConfigRepository = roleWebhookConfigRepository;
        this.webhookService = webhookService;
    }

    /**
     * Sends a summary of the slot's fresh proposals, filtered by the configured quantity threshold.
     *
     * @param runSlot scheduled hour-of-day the proposals belong to
     * @param proposals fresh open proposals produced by the slot run
     */
    public void notifyProposals(final int runSlot, final List<ForecastProposalRun> proposals) {
        final IntradayForecastProperties.Notify notify = properties.getNotify();
        if (!notify.isEnabled()) {
            return;
        }

        final List<ForecastProposalRun> relevant = proposals.stream()
                .filter(p -> p.getRecommendedQuantity() != null
                        && p.getRecommendedQuantity() >= notify.getMinRecommendedQuantity())
                .sorted((a, b) -> Integer.compare(b.getRecommendedQuantity(), a.getRecommendedQuantity()))
                .toList();
        if (relevant.isEmpty()) {
            return;
        }

        final List<String> targetRoles = notify.getTargetRoles();
        final List<RoleWebhookConfig> configs = (targetRoles == null || targetRoles.isEmpty())
                ? roleWebhookConfigRepository.findByEnabledTrue()
                : roleWebhookConfigRepository.findByRoleInAndEnabledTrue(targetRoles);
        if (configs.isEmpty()) {
            LOGGER.debug("No role webhook channel for intraday proposals (slot={}); nothing dispatched", runSlot);
            return;
        }

        final WebhookPayload payload = WebhookPayload.builder()
                .eventType(EVENT_TYPE)
                .message(buildMessage(runSlot, relevant, notify.getMaxItems()))
                .severity(WebhookPayload.Severity.INFO)
                .alertType(EVENT_TYPE)
                .timestamp(Instant.now())
                .build();

        final Set<String> sentUrls = new HashSet<>();
        for (final RoleWebhookConfig config : configs) {
            if (sentUrls.add(config.getWebhookUrl())) {
                webhookService.send(config.getProviderType().name(), config.getWebhookUrl(), payload);
            }
        }
    }

    private String buildMessage(final int runSlot, final List<ForecastProposalRun> proposals, final int maxItems) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("[AI 예측 제안] %02d시 슬롯 — 발주 제안 %d건", runSlot, proposals.size()));
        final int limit = Math.min(maxItems, proposals.size());
        for (int i = 0; i < limit; i++) {
            final ForecastProposalRun proposal = proposals.get(i);
            builder.append(String.format(
                    "%n- 상품 #%d (센터 %d / 창고 %d): 권장 %d (현재고 %d, 안전재고 %d)",
                    proposal.getProductId(),
                    proposal.getCenterId(),
                    proposal.getWarehouseId(),
                    proposal.getRecommendedQuantity(),
                    proposal.getCurrentStockQuantity(),
                    proposal.getSafetyStockQuantity()));
        }
        if (proposals.size() > limit) {
            builder.append(String.format("%n… 외 %d건", proposals.size() - limit));
        }
        return builder.toString();
    }
}
