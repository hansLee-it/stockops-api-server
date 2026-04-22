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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Persisted deterministic demand-forecast snapshot for a product and warehouse scope.
 * The snapshot stores the exact inputs used to explain recommendation outcomes later.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Entity
@Table(
        schema = "analytics",
        name = "ai_forecast_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_forecast_snapshot_scope_date",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
}
