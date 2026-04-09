package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Reason code creation request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record CreateReasonCodeRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotBlank String category
) {
}
