package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Location master entity - actual storage location within a warehouse.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "zone")
    private String zone;

    @Column(name = "shelf")
    private String shelf;

    @Column(name = "level")
    private String level;

    public Location() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return this.warehouse;
    }

    public void setWarehouse(final Warehouse warehouse) {
        this.warehouse = warehouse;
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

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getZone() {
        return this.zone;
    }

    public void setZone(final String zone) {
        this.zone = zone;
    }

    public String getShelf() {
        return this.shelf;
    }

    public void setShelf(final String shelf) {
        this.shelf = shelf;
    }

    public String getLevel() {
        return this.level;
    }

    public void setLevel(final String level) {
        this.level = level;
    }
}
