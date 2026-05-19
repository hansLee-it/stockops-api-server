package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cycle count header entity.
 * Tracks the counting session lifecycle for a single location.
 *
 * @author StockOps Team
 * @since 1.0
 * @see CycleCountItem
 */
@Entity
@Table(name = "cycle_counts")
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class CycleCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CycleCountStatus status = CycleCountStatus.PENDING;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = CycleCountStatus.PENDING;
        }
    }

    public CycleCount() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public LocalDate getCountDate() {
        return this.countDate;
    }

    public void setCountDate(final LocalDate countDate) {
        this.countDate = countDate;
    }

    public CycleCountStatus getStatus() {
        return this.status;
    }

    public void setStatus(final CycleCountStatus status) {
        this.status = status;
    }

    public Long getLocationId() {
        return this.locationId;
    }

    public void setLocationId(final Long locationId) {
        this.locationId = locationId;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCompletedBy() {
        return this.completedBy;
    }

    public void setCompletedBy(final Long completedBy) {
        this.completedBy = completedBy;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }

    public void setCompletedAt(final Instant completedAt) {
        this.completedAt = completedAt;
    }
}
