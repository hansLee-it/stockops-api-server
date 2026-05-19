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

/**
 * Outbound item entity.
 * Draft rows initially store requested product quantities and, after confirmation, are rewritten into concrete lot allocations.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "outbound_items")
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class OutboundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_id", nullable = false)
    private Long outboundId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public OutboundItem() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getOutboundId() {
        return this.outboundId;
    }

    public void setOutboundId(final Long outboundId) {
        this.outboundId = outboundId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public Long getLotId() {
        return this.lotId;
    }

    public void setLotId(final Long lotId) {
        this.lotId = lotId;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
