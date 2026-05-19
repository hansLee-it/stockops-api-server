package com.stockops.service.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for deterministic AI recommendation batches.
 *
 * @author StockOps Team
 * @since 2.0
 */
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

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getBusinessZone() {
        return this.businessZone;
    }

    public void setBusinessZone(final String businessZone) {
        this.businessZone = businessZone;
    }

    public int getForecastHistoryDays() {
        return this.forecastHistoryDays;
    }

    public void setForecastHistoryDays(final int forecastHistoryDays) {
        this.forecastHistoryDays = forecastHistoryDays;
    }

    public int getTrailingAverageDays() {
        return this.trailingAverageDays;
    }

    public void setTrailingAverageDays(final int trailingAverageDays) {
        this.trailingAverageDays = trailingAverageDays;
    }

    public int getSameWeekdayLookbackWeeks() {
        return this.sameWeekdayLookbackWeeks;
    }

    public void setSameWeekdayLookbackWeeks(final int sameWeekdayLookbackWeeks) {
        this.sameWeekdayLookbackWeeks = sameWeekdayLookbackWeeks;
    }

    public int getLeadTimeLookbackDays() {
        return this.leadTimeLookbackDays;
    }

    public void setLeadTimeLookbackDays(final int leadTimeLookbackDays) {
        this.leadTimeLookbackDays = leadTimeLookbackDays;
    }

    public int getDefaultLeadTimeDays() {
        return this.defaultLeadTimeDays;
    }

    public void setDefaultLeadTimeDays(final int defaultLeadTimeDays) {
        this.defaultLeadTimeDays = defaultLeadTimeDays;
    }

    public String getDailyCron() {
        return this.dailyCron;
    }

    public void setDailyCron(final String dailyCron) {
        this.dailyCron = dailyCron;
    }
}
