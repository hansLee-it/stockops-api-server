package com.stockops.entity;

import com.stockops.security.ScopeAssignment;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Role master entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "roles")
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Data scopes granted to all users assigned to this role.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_scope_assignments", joinColumns = @JoinColumn(name = "role_id"))
    private Set<ScopeAssignment> scopeAssignments = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Role() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<ScopeAssignment> getScopeAssignments() {
        return this.scopeAssignments;
    }

    public void setScopeAssignments(final Set<ScopeAssignment> scopeAssignments) {
        this.scopeAssignments = scopeAssignments;
    }
}
