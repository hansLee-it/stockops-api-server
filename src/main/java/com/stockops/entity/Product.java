package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

/**
 * Product master entity.
 * Soft-deleted products remain stored for audit/history while staying hidden from active queries.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "products")
@SQLRestriction("deleted = false")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "barcode", nullable = false)
    private String barcode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "expiry_managed", nullable = false)
    private boolean expiryManaged;

    @Column(name = "default_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultPrice = BigDecimal.ZERO;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Integer safetyStockQuantity = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    public Product() {
    }

    public Long getId() {
        return this.id;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(final Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public boolean isExpiryManaged() {
        return this.expiryManaged;
    }

    public void setExpiryManaged(final boolean expiryManaged) {
        this.expiryManaged = expiryManaged;
    }

    public BigDecimal getDefaultPrice() {
        return this.defaultPrice;
    }

    public void setDefaultPrice(final BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public Integer getSafetyStockQuantity() {
        return this.safetyStockQuantity;
    }

    public void setSafetyStockQuantity(final Integer safetyStockQuantity) {
        this.safetyStockQuantity = safetyStockQuantity;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }
}
