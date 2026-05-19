package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stock adjustment entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "before_quantity", nullable = false)
    private Integer beforeQuantity;

    @Column(name = "after_quantity", nullable = false)
    private Integer afterQuantity;

    @Column(name = "difference", nullable = false)
    private Integer difference;

    @Column(name = "reason_code_id")
    private Long reasonCodeId;

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    public StockAdjustment() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getInventoryId() {
        return this.inventoryId;
    }

    public void setInventoryId(final Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Integer getBeforeQuantity() {
        return this.beforeQuantity;
    }

    public void setBeforeQuantity(final Integer beforeQuantity) {
        this.beforeQuantity = beforeQuantity;
    }

    public Integer getAfterQuantity() {
        return this.afterQuantity;
    }

    public void setAfterQuantity(final Integer afterQuantity) {
        this.afterQuantity = afterQuantity;
    }

    public Integer getDifference() {
        return this.difference;
    }

    public void setDifference(final Integer difference) {
        this.difference = difference;
    }

    public Long getReasonCodeId() {
        return this.reasonCodeId;
    }

    public void setReasonCodeId(final Long reasonCodeId) {
        this.reasonCodeId = reasonCodeId;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getApprovedBy() {
        return this.approvedBy;
    }

    public void setApprovedBy(final Long approvedBy) {
        this.approvedBy = approvedBy;
    }
}
