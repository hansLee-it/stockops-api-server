package com.stockops.service.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for deterministic AI recommendation batches.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "stockops.ai")
public class AIRecommendationProperties {

    private boolean enabled = true;

    private String businessZone = "Asia/Seoul";

    private int forecastHistoryDays = 28;

    private int trailingAverageDays = 7;

    private int sameWeekdayLookbackWeeks = 4;

    private int leadTimeLookbackDays = 90;

    private int defaultLeadTimeDays = 1;

    private String dailyCron = "0 45 1 * * ?";
}
