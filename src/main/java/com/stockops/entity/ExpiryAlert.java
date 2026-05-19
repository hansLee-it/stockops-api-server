package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Expiry alert snapshot for a lot that is approaching its expiration date.
 * Records the lot quantity and alert severity calculated during the daily alert refresh.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "expiry_alerts")
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class ExpiryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "days_until_expiry", nullable = false)
    private Integer daysUntilExpiry;

    @Column(name = "alert_level", nullable = false, length = 20)
    private String alertLevel;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "is_acknowledged", nullable = false)
    private boolean acknowledged = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Initializes the created timestamp when the alert row is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public ExpiryAlert() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getLotId() {
        return this.lotId;
    }

    public void setLotId(final Long lotId) {
        this.lotId = lotId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public Integer getDaysUntilExpiry() {
        return this.daysUntilExpiry;
    }

    public void setDaysUntilExpiry(final Integer daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public String getAlertLevel() {
        return this.alertLevel;
    }

    public void setAlertLevel(final String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }

    public void setExpiryDate(final LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public boolean isAcknowledged() {
        return this.acknowledged;
    }

    public void setAcknowledged(final boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
