package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Audit log entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "target_identifier")
    private String targetIdentifier;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_by_email")
    private String performedByEmail;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        final Instant now = Instant.now();
        this.performedAt = this.performedAt == null ? now : this.performedAt;
        this.createdAt = now;
    }

    public AuditLog() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return this.entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public String getTargetIdentifier() {
        return this.targetIdentifier;
    }

    public void setTargetIdentifier(final String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public void setOldValue(final String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    public void setNewValue(final String newValue) {
        this.newValue = newValue;
    }

    public Long getPerformedBy() {
        return this.performedBy;
    }

    public void setPerformedBy(final Long performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByEmail() {
        return this.performedByEmail;
    }

    public void setPerformedByEmail(final String performedByEmail) {
        this.performedByEmail = performedByEmail;
    }

    public Instant getPerformedAt() {
        return this.performedAt;
    }

    public void setPerformedAt(final Instant performedAt) {
        this.performedAt = performedAt;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
