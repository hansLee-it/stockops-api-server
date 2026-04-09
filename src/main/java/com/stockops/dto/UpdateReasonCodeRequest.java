package com.stockops.dto;

/**
 * Reason code update request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record UpdateReasonCodeRequest(
        String name,
        String description,
        String category
) {
}
