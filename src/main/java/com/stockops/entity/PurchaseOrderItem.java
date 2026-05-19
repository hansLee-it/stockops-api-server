package com.stockops.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Purchase Order Item entity.
 * Represents individual line items in a purchase order.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "accepted_quantity")
    private Integer acceptedQuantity = 0;

    @Column(name = "cancelled_quantity")
    private Integer cancelledQuantity = 0;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "note")
    private String note;

    public PurchaseOrderItem() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return this.purchaseOrder;
    }

    public void setPurchaseOrder(final PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

    public Integer getRequestedQuantity() {
        return this.requestedQuantity;
    }

    public void setRequestedQuantity(final Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getAcceptedQuantity() {
        return this.acceptedQuantity;
    }

    public void setAcceptedQuantity(final Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getCancelledQuantity() {
        return this.cancelledQuantity;
    }

    public void setCancelledQuantity(final Integer cancelledQuantity) {
        this.cancelledQuantity = cancelledQuantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return this.totalPrice;
    }

    public void setTotalPrice(final BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(final String note) {
        this.note = note;
    }
}
