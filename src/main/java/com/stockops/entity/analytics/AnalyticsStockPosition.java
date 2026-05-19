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
 * Daily stock-position snapshot for BI analytics and downstream reorder logic.
 * Stores end-of-business-day quantities at warehouse scope with center identifiers for scoped aggregation.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "daily_stock_position",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_stock",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AnalyticsStockPosition extends BaseEntity {

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

    @Column(name = "on_hand_quantity", nullable = false)
    private Integer onHandQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "quarantined_quantity", nullable = false)
    private Integer quarantinedQuantity;

    public AnalyticsStockPosition() {
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

    public Integer getOnHandQuantity() {
        return this.onHandQuantity;
    }

    public void setOnHandQuantity(final Integer onHandQuantity) {
        this.onHandQuantity = onHandQuantity;
    }

    public Integer getAvailableQuantity() {
        return this.availableQuantity;
    }

    public void setAvailableQuantity(final Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return this.reservedQuantity;
    }

    public void setReservedQuantity(final Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getQuarantinedQuantity() {
        return this.quarantinedQuantity;
    }

    public void setQuarantinedQuantity(final Integer quarantinedQuantity) {
        this.quarantinedQuantity = quarantinedQuantity;
    }
}
