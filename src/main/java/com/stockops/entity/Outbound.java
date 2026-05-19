package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Outbound header entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "outbounds")
public class Outbound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_date", nullable = false)
    private LocalDate outboundDate;

    @Column(name = "customer")
    private String customer;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "created_by")
    private Long createdBy;

    public Outbound() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public LocalDate getOutboundDate() {
        return this.outboundDate;
    }

    public void setOutboundDate(final LocalDate outboundDate) {
        this.outboundDate = outboundDate;
    }

    public String getCustomer() {
        return this.customer;
    }

    public void setCustomer(final String customer) {
        this.customer = customer;
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
