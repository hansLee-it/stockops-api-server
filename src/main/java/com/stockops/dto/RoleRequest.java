package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Role write request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record RoleRequest(
        @NotBlank String name,
        String description
) {
}
