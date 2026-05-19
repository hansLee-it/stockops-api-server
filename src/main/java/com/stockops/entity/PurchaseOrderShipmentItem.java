package com.stockops.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Purchase Order Shipment Item entity.
 * Represents individual shipment line items.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "purchase_order_shipment_items")
public class PurchaseOrderShipmentItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    @JsonIgnore
    private PurchaseOrderShipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    @JsonIgnore
    private PurchaseOrderItem purchaseOrderItem;

    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity;

    public PurchaseOrderShipmentItem() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public PurchaseOrderShipment getShipment() {
        return this.shipment;
    }

    public void setShipment(final PurchaseOrderShipment shipment) {
        this.shipment = shipment;
    }

    public PurchaseOrderItem getPurchaseOrderItem() {
        return this.purchaseOrderItem;
    }

    public void setPurchaseOrderItem(final PurchaseOrderItem purchaseOrderItem) {
        this.purchaseOrderItem = purchaseOrderItem;
    }

    public Integer getShippedQuantity() {
        return this.shippedQuantity;
    }

    public void setShippedQuantity(final Integer shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }
}
