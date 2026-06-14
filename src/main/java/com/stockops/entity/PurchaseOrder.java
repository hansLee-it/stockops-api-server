package com.stockops.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Order header entity.
 * Represents a purchase order request from a center to ERP.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true)
    private String poNumber;

    // Nullable: store-originated requests have no center until an administrator approves and
    // designates one. Supplier-procurement orders set it at creation.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_center_id")
    private Center requestingCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_warehouse_id")
    private Warehouse targetWarehouse;

    // Set when a retail store originates the request; null for supplier-procurement orders.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_store_id")
    private Store requestingStore;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_code")
    private String supplierCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "erp_reference")
    private String erpReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "erp_responded_at")
    private LocalDateTime erpRespondedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "total_requested_amount", precision = 15, scale = 2)
    private BigDecimal totalRequestedAmount = BigDecimal.ZERO;

    @Column(name = "total_accepted_amount", precision = 15, scale = 2)
    private BigDecimal totalAcceptedAmount = BigDecimal.ZERO;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderShipment> shipments = new ArrayList<>();

    public PurchaseOrder() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Store getRequestingStore() {
        return this.requestingStore;
    }

    public void setRequestingStore(final Store requestingStore) {
        this.requestingStore = requestingStore;
    }

    public String getPoNumber() {
        return this.poNumber;
    }

    public void setPoNumber(final String poNumber) {
        this.poNumber = poNumber;
    }

    public Center getRequestingCenter() {
        return this.requestingCenter;
    }

    public void setRequestingCenter(final Center requestingCenter) {
        this.requestingCenter = requestingCenter;
    }

    public Warehouse getTargetWarehouse() {
        return this.targetWarehouse;
    }

    public void setTargetWarehouse(final Warehouse targetWarehouse) {
        this.targetWarehouse = targetWarehouse;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierCode() {
        return this.supplierCode;
    }

    public void setSupplierCode(final String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public PurchaseOrderStatus getStatus() {
        return this.status;
    }

    public void setStatus(final PurchaseOrderStatus status) {
        this.status = status;
    }

    public String getErpReference() {
        return this.erpReference;
    }

    public void setErpReference(final String erpReference) {
        this.erpReference = erpReference;
    }

    public User getRequestedBy() {
        return this.requestedBy;
    }

    public void setRequestedBy(final User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public LocalDateTime getRequestedAt() {
        return this.requestedAt;
    }

    public void setRequestedAt(final LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getErpRespondedAt() {
        return this.erpRespondedAt;
    }

    public void setErpRespondedAt(final LocalDateTime erpRespondedAt) {
        this.erpRespondedAt = erpRespondedAt;
    }

    public String getCancelReason() {
        return this.cancelReason;
    }

    public void setCancelReason(final String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public BigDecimal getTotalRequestedAmount() {
        return this.totalRequestedAmount;
    }

    public void setTotalRequestedAmount(final BigDecimal totalRequestedAmount) {
        this.totalRequestedAmount = totalRequestedAmount;
    }

    public BigDecimal getTotalAcceptedAmount() {
        return this.totalAcceptedAmount;
    }

    public void setTotalAcceptedAmount(final BigDecimal totalAcceptedAmount) {
        this.totalAcceptedAmount = totalAcceptedAmount;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public List<PurchaseOrderItem> getItems() {
        return this.items;
    }

    public void setItems(final List<PurchaseOrderItem> items) {
        this.items = items;
    }

    public List<PurchaseOrderShipment> getShipments() {
        return this.shipments;
    }

    public void setShipments(final List<PurchaseOrderShipment> shipments) {
        this.shipments = shipments;
    }
}
