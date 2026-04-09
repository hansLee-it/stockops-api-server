package com.stockops.dto;

/**
 * User update request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record UpdateUserRequest(
        String name,
        String role
) {
}
