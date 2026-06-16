package com.stockops.ai.bedrock;

import java.time.LocalDate;
import java.time.ZoneId;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Schedules a daily pre-warm of the Bedrock operational summary for today's date.
 * By generating the summary at 08:00 KST before business hours, the first user
 * who hits the ops-summary endpoint finds a cached result instead of waiting
 * for a live LLM call.
 *
 * <p>Enabled only when {@code stockops.ai.bedrock.ops-summary-schedule.enabled=true}.
 * Disabled by default to prevent unintended Bedrock calls in local/test environments.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
@ConditionalOnProperty(
        name = "stockops.ai.bedrock.ops-summary-schedule.enabled",
        havingValue = "true")
public class AiOpsSummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiOpsSummaryScheduler.class);

    private final BedrockAiFacade bedrockAiFacade;

    public AiOpsSummaryScheduler(final BedrockAiFacade bedrockAiFacade) {
        this.bedrockAiFacade = bedrockAiFacade;
    }

    /**
     * Pre-warms the ops summary cache for today's date at 08:00 KST.
     * Errors are logged and swallowed to prevent scheduler thread termination.
     */
    @Scheduled(
            cron = "${stockops.ai.bedrock.ops-summary-schedule.cron:0 0 8 * * ?}",
            zone = "${stockops.ai.bedrock.ops-summary-schedule.zone:Asia/Seoul}")
    @SchedulerLock(name = "aiOpsSummaryPrewarm", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void generateDailyOpsSummaries() {
        final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[AI] Daily ops summary batch started for {}", today);
        try {
            bedrockAiFacade.summarizeOperations(today, null, null);
            log.info("[AI] Daily ops summary batch completed for {}", today);
        } catch (final RuntimeException e) {
            log.error("[AI] Daily ops summary batch failed for {}: {}", today, e.getMessage());
        }
    }
}
