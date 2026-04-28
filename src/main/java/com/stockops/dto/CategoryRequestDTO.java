package com.stockops.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Category create/update request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record CategoryRequestDTO(
        @NotBlank String name,
        @NotBlank String code,
        Long parentId,
        @Min(1) @Max(3) Integer level,
        Integer sortOrder,
        Boolean active
) {
}