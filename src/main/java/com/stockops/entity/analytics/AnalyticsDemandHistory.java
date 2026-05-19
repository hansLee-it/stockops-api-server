package com.stockops.entity.analytics;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * Daily confirmed-demand analytics row for a product and warehouse scope.
 * This table is the canonical demand source for BI and AI because it only stores confirmed outbound demand.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "daily_demand_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_demand",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AnalyticsDemandHistory extends BaseEntity {

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

    @Column(name = "confirmed_outbound_quantity", nullable = false)
    private Integer confirmedOutboundQuantity;

    @Column(name = "confirmed_outbound_event_count", nullable = false)
    private Integer confirmedOutboundEventCount;

    @Column(name = "insufficient_history", nullable = false)
    private boolean insufficientHistory;

    public AnalyticsDemandHistory() {
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

    public Integer getConfirmedOutboundQuantity() {
        return this.confirmedOutboundQuantity;
    }

    public void setConfirmedOutboundQuantity(final Integer confirmedOutboundQuantity) {
        this.confirmedOutboundQuantity = confirmedOutboundQuantity;
    }

    public Integer getConfirmedOutboundEventCount() {
        return this.confirmedOutboundEventCount;
    }

    public void setConfirmedOutboundEventCount(final Integer confirmedOutboundEventCount) {
        this.confirmedOutboundEventCount = confirmedOutboundEventCount;
    }

    public boolean isInsufficientHistory() {
        return this.insufficientHistory;
    }

    public void setInsufficientHistory(final boolean insufficientHistory) {
        this.insufficientHistory = insufficientHistory;
    }
}
