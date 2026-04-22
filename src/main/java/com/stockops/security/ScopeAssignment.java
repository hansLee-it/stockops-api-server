package com.stockops.security;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded scope assignment used by users and roles.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ScopeAssignment {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scope;

    @Column(name = "center_id")
    private Long centerId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    /**
     * Creates a global scope assignment.
     *
     * @return global assignment
     */
    public static ScopeAssignment global() {
        return new ScopeAssignment(ScopeType.GLOBAL, null, null);
    }
}
