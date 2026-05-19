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
 * Daily expiry-waste source row derived from automatic quarantine events.
 * Quantities are stored per product and warehouse so reporting can aggregate waste without recomputing domain joins.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(
        schema = "analytics",
        name = "daily_expiry_waste",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_expiry_waste",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
public class AnalyticsExpiryWaste extends BaseEntity {

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

    @Column(name = "quarantined_quantity", nullable = false)
    private Integer quarantinedQuantity;

    @Column(name = "quarantined_lot_count", nullable = false)
    private Integer quarantinedLotCount;

    public AnalyticsExpiryWaste() {
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

    public Integer getQuarantinedQuantity() {
        return this.quarantinedQuantity;
    }

    public void setQuarantinedQuantity(final Integer quarantinedQuantity) {
        this.quarantinedQuantity = quarantinedQuantity;
    }

    public Integer getQuarantinedLotCount() {
        return this.quarantinedLotCount;
    }

    public void setQuarantinedLotCount(final Integer quarantinedLotCount) {
        this.quarantinedLotCount = quarantinedLotCount;
    }
}
