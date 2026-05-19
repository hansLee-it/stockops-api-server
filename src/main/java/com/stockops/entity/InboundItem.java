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
 * Inbound item entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "inbound_items")
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class InboundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_id", nullable = false)
    private Long inboundId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public InboundItem() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getInboundId() {
        return this.inboundId;
    }

    public void setInboundId(final Long inboundId) {
        this.inboundId = inboundId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public String getLotNumber() {
        return this.lotNumber;
    }

    public void setLotNumber(final String lotNumber) {
        this.lotNumber = lotNumber;
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

    public Long getLocationId() {
        return this.locationId;
    }

    public void setLocationId(final Long locationId) {
        this.locationId = locationId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
