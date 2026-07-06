package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Create/update request payload for a center. Id and audit fields are never accepted from clients.
 *
 * @param code center code (immutable after creation; validated on update)
 * @param name center display name
 * @param address optional postal address
 * @param phone optional contact phone
 * @author StockOps Team
 * @since 2.0
 */
public record CenterRequest(
        @NotBlank String code,
        @NotBlank String name,
        String address,
        String phone
) {
}
