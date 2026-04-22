package com.stockops.dto;

import java.util.List;

/**
 * User update request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record UpdateUserRequest(
        String name,
        String role,
        List<ScopeAssignmentRequest> scopeAssignments
) {
}
