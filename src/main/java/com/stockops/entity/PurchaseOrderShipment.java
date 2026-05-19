package com.stockops.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Order Shipment entity.
 * Represents shipment information from ERP.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "purchase_order_shipments")
public class PurchaseOrderShipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    @Column(name = "shipment_number", nullable = false)
    private String shipmentNumber;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "eta_date")
    private LocalDate etaDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderShipmentItem> shipmentItems = new ArrayList<>();

    public PurchaseOrderShipment() {
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

    public String getShipmentNumber() {
        return this.shipmentNumber;
    }

    public void setShipmentNumber(final String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getCarrier() {
        return this.carrier;
    }

    public void setCarrier(final String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return this.trackingNumber;
    }

    public void setTrackingNumber(final String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public ShipmentStatus getStatus() {
        return this.status;
    }

    public void setStatus(final ShipmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getShippedAt() {
        return this.shippedAt;
    }

    public void setShippedAt(final LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDate getEtaDate() {
        return this.etaDate;
    }

    public void setEtaDate(final LocalDate etaDate) {
        this.etaDate = etaDate;
    }

    public LocalDateTime getDeliveredAt() {
        return this.deliveredAt;
    }

    public void setDeliveredAt(final LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public List<PurchaseOrderShipmentItem> getShipmentItems() {
        return this.shipmentItems;
    }

    public void setShipmentItems(final List<PurchaseOrderShipmentItem> shipmentItems) {
        this.shipmentItems = shipmentItems;
    }
}
