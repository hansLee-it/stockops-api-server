package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Create/update request payload for a warehouse. Id, center linkage, and audit fields are never
 * accepted from the request body (center comes from the path on create).
 *
 * @param code warehouse code
 * @param name warehouse display name
 * @param address optional postal address
 * @param phone optional contact phone
 * @author StockOps Team
 * @since 2.0
 */
public record WarehouseRequest(
        @NotBlank String code,
        @NotBlank String name,
        String address,
        String phone
) {
}
