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
 * Daily purchase-order lead-time source row.
 * Lead time is tracked as requested-to-ERP-response hours so downstream analytics can compute averages deterministically.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "daily_purchase_order_lead_time",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_po_lead_time",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AnalyticsPurchaseOrderLeadTime extends BaseEntity {

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

    @Column(name = "purchase_order_count", nullable = false)
    private Integer purchaseOrderCount;

    @Column(name = "lead_time_sample_count", nullable = false)
    private Integer leadTimeSampleCount;

    @Column(name = "total_lead_time_hours", nullable = false)
    private Long totalLeadTimeHours;

    public AnalyticsPurchaseOrderLeadTime() {
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

    public Integer getPurchaseOrderCount() {
        return this.purchaseOrderCount;
    }

    public void setPurchaseOrderCount(final Integer purchaseOrderCount) {
        this.purchaseOrderCount = purchaseOrderCount;
    }

    public Integer getLeadTimeSampleCount() {
        return this.leadTimeSampleCount;
    }

    public void setLeadTimeSampleCount(final Integer leadTimeSampleCount) {
        this.leadTimeSampleCount = leadTimeSampleCount;
    }

    public Long getTotalLeadTimeHours() {
        return this.totalLeadTimeHours;
    }

    public void setTotalLeadTimeHours(final Long totalLeadTimeHours) {
        this.totalLeadTimeHours = totalLeadTimeHours;
    }
}
