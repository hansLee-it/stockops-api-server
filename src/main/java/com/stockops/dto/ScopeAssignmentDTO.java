package com.stockops.dto;

import com.stockops.security.ScopeType;

/**
 * Scope assignment response payload.
 *
 * @param scope scope level
 * @param centerId scoped center id when applicable
 * @param warehouseId scoped warehouse id when applicable
 * @author StockOps Team
 * @since 2.0
 */
public record ScopeAssignmentDTO(
        ScopeType scope,
        Long centerId,
        Long warehouseId
) {
}
