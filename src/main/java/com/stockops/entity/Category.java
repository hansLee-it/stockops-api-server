package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Category entity for product classification hierarchy.
 * Supports 3-level classification: 대분류 (Level 1) → 중분류 (Level 2) → 소분류 (Level 3).
 * Self-referencing entity for parent-child relationships.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "categories")
@SQLRestriction("active = true")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    /**
     * Check if this is a root category (no parent).
     *
     * @return true if root category
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Check if this category has children.
     *
     * @return true if has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Creates a new Category with minimal required fields.
     *
     * @param name category name
     * @param code unique category code
     * @param level category level (1, 2, or 3)
     */
    public Category(String name, String code, Integer level) {
        this.name = name;
        this.code = code;
        this.level = level;
        this.active = true;
        this.sortOrder = 0;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public Category getParent() {
        return this.parent;
    }

    public void setParent(final Category parent) {
        this.parent = parent;
    }

    public List<Category> getChildren() {
        return this.children;
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public Category() {
    }
}