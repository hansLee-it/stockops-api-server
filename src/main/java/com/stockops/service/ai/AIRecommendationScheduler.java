package com.stockops.service.ai;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduler for AI recommendation snapshots.
 * The job runs after analytics refresh so forecasting reads the latest deterministic aggregates.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIRecommendationScheduler {

    private final AIRecommendationProperties properties;
    private final AIRecommendationService aiRecommendationService;

    /**
     * Runs the daily AI reorder recommendation batch for the current business date.
     */
    @Scheduled(cron = "${stockops.ai.daily-cron:0 45 1 * * ?}", zone = "${stockops.ai.business-zone:Asia/Seoul}")
    public void generateDailyRecommendations() {
        if (!properties.isEnabled()) {
            log.info("Skipping AI recommendation batch because stockops.ai.enabled=false");
            return;
        }

        aiRecommendationService.generateRecommendationsForBusinessDate(LocalDate.now(ZoneId.of(properties.getBusinessZone())));
    }
}
