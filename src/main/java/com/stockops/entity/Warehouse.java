package com.stockops.entity;

import jakarta.persistence.*;

/**
 * Warehouse entity - physical building that belongs to a center.
 * Actual inbound/outbound operations happen here.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "warehouses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"center_id", "code"})
})
public class Warehouse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;

    @Column(name = "closure_reason")
    private String closureReason;

    @Column(name = "closed_at")
    private java.time.Instant closedAt;

    public Warehouse() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Center getCenter() {
        return this.center;
    }

    public void setCenter(final Center center) {
        this.center = center;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public WarehouseStatus getStatus() {
        return this.status;
    }

    public void setStatus(final WarehouseStatus status) {
        this.status = status;
    }

    public String getClosureReason() {
        return this.closureReason;
    }

    public void setClosureReason(final String closureReason) {
        this.closureReason = closureReason;
    }

    public java.time.Instant getClosedAt() {
        return this.closedAt;
    }

    public void setClosedAt(final java.time.Instant closedAt) {
        this.closedAt = closedAt;
    }
}
