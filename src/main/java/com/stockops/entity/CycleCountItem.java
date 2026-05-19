package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Cycle count detail entity.
 * Stores the expected inventory balance and the final counted result for one inventory row.
 *
 * @author StockOps Team
 * @since 1.0
 * @see CycleCount
 */
@Entity
@Table(name = "cycle_count_items")
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class CycleCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_count_id", nullable = false)
    private Long cycleCountId;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "variance")
    private Integer variance;

    @Column(name = "counted_by")
    private Long countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(name = "notes")
    private String notes;

    public CycleCountItem() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getCycleCountId() {
        return this.cycleCountId;
    }

    public void setCycleCountId(final Long cycleCountId) {
        this.cycleCountId = cycleCountId;
    }

    public Long getInventoryId() {
        return this.inventoryId;
    }

    public void setInventoryId(final Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Integer getExpectedQuantity() {
        return this.expectedQuantity;
    }

    public void setExpectedQuantity(final Integer expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Integer getActualQuantity() {
        return this.actualQuantity;
    }

    public void setActualQuantity(final Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public Integer getVariance() {
        return this.variance;
    }

    public void setVariance(final Integer variance) {
        this.variance = variance;
    }

    public Long getCountedBy() {
        return this.countedBy;
    }

    public void setCountedBy(final Long countedBy) {
        this.countedBy = countedBy;
    }

    public Instant getCountedAt() {
        return this.countedAt;
    }

    public void setCountedAt(final Instant countedAt) {
        this.countedAt = countedAt;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }
}
