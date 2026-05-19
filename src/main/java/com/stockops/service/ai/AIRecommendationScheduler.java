package com.stockops.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduler for AI recommendation snapshots.
 * The job runs after analytics refresh so forecasting reads the latest deterministic aggregates.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
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

    private static final Logger log = LoggerFactory.getLogger(AIRecommendationScheduler.class);

    public AIRecommendationScheduler(final AIRecommendationProperties properties, final AIRecommendationService aiRecommendationService) {
        this.properties = properties;
        this.aiRecommendationService = aiRecommendationService;
    }
}
