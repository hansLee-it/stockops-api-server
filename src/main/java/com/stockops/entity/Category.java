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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@Entity
@Table(name = "categories")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("active = true")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
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
}