package com.stockops.dto;

import com.stockops.security.ScopeType;
import jakarta.validation.constraints.NotNull;

/**
 * Scope assignment write payload.
 *
 * @param scope scope level
 * @param centerId scoped center id when applicable
 * @param warehouseId scoped warehouse id when applicable
 * @author StockOps Team
 * @since 2.0
 */
public record ScopeAssignmentRequest(
        @NotNull ScopeType scope,
        Long centerId,
        Long warehouseId
) {
}
