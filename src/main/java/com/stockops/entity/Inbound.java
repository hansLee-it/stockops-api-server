package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Inbound header entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "inbounds")
public class Inbound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_date", nullable = false)
    private LocalDate inboundDate;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "created_by")
    private Long createdBy;

    public Inbound() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public LocalDate getInboundDate() {
        return this.inboundDate;
    }

    public void setInboundDate(final LocalDate inboundDate) {
        this.inboundDate = inboundDate;
    }

    public String getSupplier() {
        return this.supplier;
    }

    public void setSupplier(final String supplier) {
        this.supplier = supplier;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Integer getTotalQuantity() {
        return this.totalQuantity;
    }

    public void setTotalQuantity(final Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }
}
