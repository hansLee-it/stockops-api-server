package com.stockops.service.ai;

import com.stockops.entity.ai.ForecastProposalRun;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the intraday forecast slots (default 10:00 and 15:00 KST) and pushes fresh proposals to Teams.
 *
 * <p>Registered only when {@code stockops.ai.intraday.enabled=true} so it never fires live forecasts
 * (or external AI calls) in local/test environments. {@code @SchedulerLock} ensures a single
 * load-balanced instance runs each slot. Exceptions are logged and swallowed so the scheduler thread
 * survives a failed run.
 *
 * @author StockOps Team
 * @since 2.4
 */
@Component
@ConditionalOnProperty(name = "stockops.ai.intraday.enabled", havingValue = "true")
public class IntradayForecastScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntradayForecastScheduler.class);

    private final IntradayForecastProperties properties;
    private final IntradayForecastService intradayForecastService;
    private final IntradayProposalNotifier notifier;

    public IntradayForecastScheduler(final IntradayForecastProperties properties,
            final IntradayForecastService intradayForecastService,
            final IntradayProposalNotifier notifier) {
        this.properties = properties;
        this.intradayForecastService = intradayForecastService;
        this.notifier = notifier;
    }

    /**
     * Generates proposals for the current slot. The slot is the run's hour-of-day in the business
     * zone, which matches the configured cron hours (default 10 and 15).
     */
    @Scheduled(
            cron = "${stockops.ai.intraday.slots-cron:0 0 10,15 * * ?}",
            zone = "${stockops.ai.intraday.business-zone:Asia/Seoul}")
    @SchedulerLock(name = "intradayForecast", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void runSlot() {
        final ZoneId zone = ZoneId.of(properties.getBusinessZone());
        final LocalDate businessDate = LocalDate.now(zone);
        final int runSlot = LocalDateTime.now(zone).getHour();
        final Instant runAt = Instant.now();

        log.info("[AI] Intraday forecast slot {} started for {}", runSlot, businessDate);
        try {
            final List<ForecastProposalRun> proposals =
                    intradayForecastService.generateProposalsForSlot(businessDate, runSlot, runAt);
            notifier.notifyProposals(runSlot, proposals);
            log.info("[AI] Intraday forecast slot {} completed for {} ({} proposals)",
                    runSlot, businessDate, proposals.size());
        } catch (final RuntimeException e) {
            log.error("[AI] Intraday forecast slot {} failed for {}: {}", runSlot, businessDate, e.getMessage(), e);
        }
    }

    /**
     * Daily sweep that flips proposals past their actionable window to EXPIRED.
     */
    @Scheduled(
            cron = "${stockops.ai.intraday.expiry-cron:0 30 0 * * ?}",
            zone = "${stockops.ai.intraday.business-zone:Asia/Seoul}")
    @SchedulerLock(name = "intradayProposalExpiry", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void expireStaleProposals() {
        try {
            intradayForecastService.expireStaleProposals();
        } catch (final RuntimeException e) {
            log.error("[AI] Intraday proposal expiry sweep failed: {}", e.getMessage(), e);
        }
    }
}
