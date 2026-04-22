package com.stockops.dto;

import java.time.Instant;

/**
 * Role response payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record RoleDTO(
        Long id,
        String name,
        String description,
        ScopeMetadataDTO scopeMetadata,
        Instant createdAt
) {
}
