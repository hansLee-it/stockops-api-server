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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Persisted AI reorder recommendation snapshot for a product and warehouse scope.
 * Recommendations stay advisory until an authorized user approves them into a draft purchase order.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "ai_reorder_recommendations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_recommendation_scope_date",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AIRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_snapshot_id", nullable = false, unique = true)
    private AIForecastSnapshot forecastSnapshot;

    @Column(name = "current_stock_quantity", nullable = false)
    private Integer currentStockQuantity = 0;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Integer safetyStockQuantity = 0;

    @Column(name = "recommended_quantity", nullable = false)
    private Integer recommendedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AIRecommendationStatus status = AIRecommendationStatus.NO_ACTION;

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

    public AIRecommendation() {
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

    public AIForecastSnapshot getForecastSnapshot() {
        return this.forecastSnapshot;
    }

    public void setForecastSnapshot(final AIForecastSnapshot forecastSnapshot) {
        this.forecastSnapshot = forecastSnapshot;
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

    public AIRecommendationStatus getStatus() {
        return this.status;
    }

    public void setStatus(final AIRecommendationStatus status) {
        this.status = status;
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
}
