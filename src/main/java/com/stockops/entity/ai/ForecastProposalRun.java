package com.stockops.entity.ai;

import com.stockops.entity.BaseEntity;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One intraday forecast proposal for a product/center/warehouse scope, recorded per scheduled slot.
 *
 * <p>Unlike {@link AIRecommendation} (latest-only, one row per business date), proposal runs
 * accumulate: the unique key includes {@code run_slot} so each slot of the day keeps its own row,
 * letting reviewers see how a scope's reorder proposal shifted as live stock was consumed.
 *
 * <p>A proposal can be approved into a draft purchase order or rejected only while
 * {@link #isActionable(Instant)} holds (now &le; {@code actionableUntil}); afterwards it is
 * history only.
 *
 * @author StockOps Team
 * @since 2.4
 */
@Entity
@Table(
        schema = "analytics",
        name = "forecast_proposal_runs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_forecast_proposal_run_scope",
                columnNames = {"business_date", "run_slot", "product_id", "center_id", "warehouse_id"}))
public class ForecastProposalRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    /** Scheduled hour-of-day in the business zone the run belongs to (e.g. 10, 15). */
    @Column(name = "run_slot", nullable = false)
    private Integer runSlot;

    @Column(name = "run_at", nullable = false)
    private Instant runAt;

    @Column(name = "actionable_until", nullable = false)
    private Instant actionableUntil;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "current_stock_quantity", nullable = false)
    private Integer currentStockQuantity = 0;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Integer safetyStockQuantity = 0;

    @Column(name = "recommended_quantity", nullable = false)
    private Integer recommendedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ForecastProposalStatus status = ForecastProposalStatus.PROPOSED;

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

    @Column(name = "demand_event_count", nullable = false)
    private Integer demandEventCount = 0;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "explanation_summary", length = 500)
    private String explanationSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_purchase_order_id")
    private PurchaseOrder approvedPurchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id")
    private User rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public ForecastProposalRun() {
    }

    /**
     * Whether this proposal can still be approved or rejected at the supplied instant.
     * Only open ({@link ForecastProposalStatus#PROPOSED}) proposals within the actionable
     * window are decidable.
     *
     * @param now reference instant
     * @return true when a decision is still allowed
     */
    public boolean isActionable(final Instant now) {
        return status == ForecastProposalStatus.PROPOSED
                && actionableUntil != null
                && !now.isAfter(actionableUntil);
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

    public Integer getRunSlot() {
        return this.runSlot;
    }

    public void setRunSlot(final Integer runSlot) {
        this.runSlot = runSlot;
    }

    public Instant getRunAt() {
        return this.runAt;
    }

    public void setRunAt(final Instant runAt) {
        this.runAt = runAt;
    }

    public Instant getActionableUntil() {
        return this.actionableUntil;
    }

    public void setActionableUntil(final Instant actionableUntil) {
        this.actionableUntil = actionableUntil;
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

    public Integer getCurrentStockQuantity() {
        return this.currentStockQuantity;
    }

    public void setCurrentStockQuantity(final Integer currentStockQuantity) {
        this.currentStockQuantity = currentStockQuantity;
    }

    public Integer getSafetyStockQuantity() {
        return this.safetyStockQuantity;
    }

    public void setSafetyStockQuantity(final Integer safetyStockQuantity) {
        this.safetyStockQuantity = safetyStockQuantity;
    }

    public Integer getRecommendedQuantity() {
        return this.recommendedQuantity;
    }

    public void setRecommendedQuantity(final Integer recommendedQuantity) {
        this.recommendedQuantity = recommendedQuantity;
    }

    public ForecastProposalStatus getStatus() {
        return this.status;
    }

    public void setStatus(final ForecastProposalStatus status) {
        this.status = status;
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

    public Integer getDemandEventCount() {
        return this.demandEventCount;
    }

    public void setDemandEventCount(final Integer demandEventCount) {
        this.demandEventCount = demandEventCount;
    }

    public String getModelVersion() {
        return this.modelVersion;
    }

    public void setModelVersion(final String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getExplanationSummary() {
        return this.explanationSummary;
    }

    public void setExplanationSummary(final String explanationSummary) {
        this.explanationSummary = explanationSummary;
    }

    public PurchaseOrder getApprovedPurchaseOrder() {
        return this.approvedPurchaseOrder;
    }

    public void setApprovedPurchaseOrder(final PurchaseOrder approvedPurchaseOrder) {
        this.approvedPurchaseOrder = approvedPurchaseOrder;
    }

    public User getApprovedBy() {
        return this.approvedBy;
    }

    public void setApprovedBy(final User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public void setApprovedAt(final Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public User getRejectedBy() {
        return this.rejectedBy;
    }

    public void setRejectedBy(final User rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public Instant getRejectedAt() {
        return this.rejectedAt;
    }

    public void setRejectedAt(final Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public void setRejectionReason(final String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
