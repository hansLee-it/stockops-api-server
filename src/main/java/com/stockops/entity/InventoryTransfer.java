package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Inventory transfer entity.
 * Represents a request to move stock from one location to another within the same center.
 *
 * @author StockOps Team
 * @since 2.0
 * @see InventoryTransferStatus
 * @see Inventory
 */
@Entity
@Table(name = "inventory_transfers")
public class InventoryTransfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "from_location_id", nullable = false)
    private Long fromLocationId;

    @Column(name = "to_location_id", nullable = false)
    private Long toLocationId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InventoryTransferStatus status = InventoryTransferStatus.REQUESTED;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "notes")
    private String notes;

    public InventoryTransfer() {
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

    public Long getLotId() {
        return this.lotId;
    }

    public void setLotId(final Long lotId) {
        this.lotId = lotId;
    }

    public Long getFromLocationId() {
        return this.fromLocationId;
    }

    public void setFromLocationId(final Long fromLocationId) {
        this.fromLocationId = fromLocationId;
    }

    public Long getToLocationId() {
        return this.toLocationId;
    }

    public void setToLocationId(final Long toLocationId) {
        this.toLocationId = toLocationId;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public InventoryTransferStatus getStatus() {
        return this.status;
    }

    public void setStatus(final InventoryTransferStatus status) {
        this.status = status;
    }

    public Long getRequestedBy() {
        return this.requestedBy;
    }

    public void setRequestedBy(final Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Long getCompletedBy() {
        return this.completedBy;
    }

    public void setCompletedBy(final Long completedBy) {
        this.completedBy = completedBy;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }
}
