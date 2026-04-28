package com.stockops.dto;

import java.time.Instant;
import java.util.List;

/**
 * Category response payload with nested children for tree representation.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record CategoryDTO(
        Long id,
        String name,
        String code,
        Integer level,
        Integer sortOrder,
        boolean active,
        Long parentId,
        List<CategoryDTO> children,
        Instant createdAt,
        Instant updatedAt
) {
}