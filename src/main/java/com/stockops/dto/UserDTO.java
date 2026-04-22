package com.stockops.dto;

import java.time.Instant;

/**
 * User response payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record UserDTO(
        Long id,
        String email,
        String name,
        String role,
        ScopeMetadataDTO scopeMetadata,
        Instant createdAt,
        Instant updatedAt
) {
}
