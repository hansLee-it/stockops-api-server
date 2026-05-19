package com.stockops.entity.ai;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Persisted deterministic demand-forecast snapshot for a product and warehouse scope.
 * The snapshot stores the exact inputs used to explain recommendation outcomes later.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "ai_forecast_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_forecast_snapshot_scope_date",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AIForecastSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "forecast_start_date", nullable = false)
    private LocalDate forecastStartDate;

    @Column(name = "forecast_end_date", nullable = false)
    private LocalDate forecastEndDate;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "trailing_seven_day_average", nullable = false, precision = 12, scale = 2)
    private BigDecimal trailingSevenDayAverage = BigDecimal.ZERO;

    @Column(name = "same_weekday_average", nullable = false, precision = 12, scale = 2)
    private BigDecimal sameWeekdayAverage = BigDecimal.ZERO;

    @Column(name = "weighted_daily_demand", nullable = false, precision = 12, scale = 2)
    private BigDecimal weightedDailyDemand = BigDecimal.ZERO;

    @Column(name = "seven_day_forecast_quantity", nullable = false)
    private Integer sevenDayForecastQuantity = 0;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays = 1;

    @Column(name = "lead_time_demand_quantity", nullable = false)
    private Integer leadTimeDemandQuantity = 0;

    @Column(name = "history_days_considered", nullable = false)
    private Integer historyDaysConsidered = 0;

    @Column(name = "demand_event_count", nullable = false)
    private Integer demandEventCount = 0;

    @Column(name = "insufficient_history", nullable = false)
    private boolean insufficientHistory;

    @Column(name = "explanation_summary", length = 500)
    private String explanationSummary;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion = "statistical";

    public AIForecastSnapshot() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public LocalDate getBusinessDate() {
        return this.businessDate;
    }

    public void setBusinessDate(final LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public LocalDate getForecastStartDate() {
        return this.forecastStartDate;
    }

    public void setForecastStartDate(final LocalDate forecastStartDate) {
        this.forecastStartDate = forecastStartDate;
    }

    public LocalDate getForecastEndDate() {
        return this.forecastEndDate;
    }

    public void setForecastEndDate(final LocalDate forecastEndDate) {
        this.forecastEndDate = forecastEndDate;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public Long getCenterId() {
        return this.centerId;
    }

    public void setCenterId(final Long centerId) {
        this.centerId = centerId;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BigDecimal getTrailingSevenDayAverage() {
        return this.trailingSevenDayAverage;
    }

    public void setTrailingSevenDayAverage(final BigDecimal trailingSevenDayAverage) {
        this.trailingSevenDayAverage = trailingSevenDayAverage;
    }

    public BigDecimal getSameWeekdayAverage() {
        return this.sameWeekdayAverage;
    }

    public void setSameWeekdayAverage(final BigDecimal sameWeekdayAverage) {
        this.sameWeekdayAverage = sameWeekdayAverage;
    }

    public BigDecimal getWeightedDailyDemand() {
        return this.weightedDailyDemand;
    }

    public void setWeightedDailyDemand(final BigDecimal weightedDailyDemand) {
        this.weightedDailyDemand = weightedDailyDemand;
    }

    public Integer getSevenDayForecastQuantity() {
        return this.sevenDayForecastQuantity;
    }

    public void setSevenDayForecastQuantity(final Integer sevenDayForecastQuantity) {
        this.sevenDayForecastQuantity = sevenDayForecastQuantity;
    }

    public Integer getLeadTimeDays() {
        return this.leadTimeDays;
    }

    public void setLeadTimeDays(final Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public Integer getLeadTimeDemandQuantity() {
        return this.leadTimeDemandQuantity;
    }

    public void setLeadTimeDemandQuantity(final Integer leadTimeDemandQuantity) {
        this.leadTimeDemandQuantity = leadTimeDemandQuantity;
    }

    public Integer getHistoryDaysConsidered() {
        return this.historyDaysConsidered;
    }

    public void setHistoryDaysConsidered(final Integer historyDaysConsidered) {
        this.historyDaysConsidered = historyDaysConsidered;
    }

    public Integer getDemandEventCount() {
        return this.demandEventCount;
    }

    public void setDemandEventCount(final Integer demandEventCount) {
        this.demandEventCount = demandEventCount;
    }

    public boolean isInsufficientHistory() {
        return this.insufficientHistory;
    }

    public void setInsufficientHistory(final boolean insufficientHistory) {
        this.insufficientHistory = insufficientHistory;
    }

    public String getExplanationSummary() {
        return this.explanationSummary;
    }

    public void setExplanationSummary(final String explanationSummary) {
        this.explanationSummary = explanationSummary;
    }

    public String getModelVersion() {
        return this.modelVersion;
    }

    public void setModelVersion(final String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
