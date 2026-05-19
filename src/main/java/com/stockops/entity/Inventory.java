package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Inventory balance entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(
        name = "inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_product_location_lot",
                columnNames = {"product_id", "location_id", "lot_id"}))
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InventoryStatus status = InventoryStatus.ACTIVE;

    public Inventory() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public Long getLocationId() {
        return this.locationId;
    }

    public void setLocationId(final Long locationId) {
        this.locationId = locationId;
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

    public Integer getReservedQuantity() {
        return this.reservedQuantity;
    }

    public void setReservedQuantity(final Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public InventoryStatus getStatus() {
        return this.status;
    }

    public void setStatus(final InventoryStatus status) {
        this.status = status;
    }
}
