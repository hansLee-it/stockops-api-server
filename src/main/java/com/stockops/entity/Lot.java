package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Lot entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "lots")
public class Lot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LotStatus status = LotStatus.ACTIVE;

    public Lot() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getLotNumber() {
        return this.lotNumber;
    }

    public void setLotNumber(final String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }

    public void setExpiryDate(final LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getReceivedDate() {
        return this.receivedDate;
    }

    public void setReceivedDate(final LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public LotStatus getStatus() {
        return this.status;
    }

    public void setStatus(final LotStatus status) {
        this.status = status;
    }
}
