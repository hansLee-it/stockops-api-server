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
 * Daily fill-rate source row for purchase-order reporting.
 * Requested, accepted, cancelled, and shipped quantities are stored without precomputed percentages so reporting stays reusable.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "daily_fill_rate_source",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_fill_rate",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AnalyticsFillRateSource extends BaseEntity {

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

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;

    @Column(name = "cancelled_quantity", nullable = false)
    private Integer cancelledQuantity;

    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity;

    public AnalyticsFillRateSource() {
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

    public Integer getRequestedQuantity() {
        return this.requestedQuantity;
    }

    public void setRequestedQuantity(final Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getAcceptedQuantity() {
        return this.acceptedQuantity;
    }

    public void setAcceptedQuantity(final Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getCancelledQuantity() {
        return this.cancelledQuantity;
    }

    public void setCancelledQuantity(final Integer cancelledQuantity) {
        this.cancelledQuantity = cancelledQuantity;
    }

    public Integer getShippedQuantity() {
        return this.shippedQuantity;
    }

    public void setShippedQuantity(final Integer shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }
}
