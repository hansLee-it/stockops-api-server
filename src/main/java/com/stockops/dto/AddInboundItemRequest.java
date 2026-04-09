package com.stockops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Inbound item creation request payload.
 *
 * @param productId product identifier
 * @param lotNumber lot number
 * @param expiryDate expiry date for the lot
 * @param quantity inbound quantity
 * @param locationId destination location identifier
 * @author StockOps Team
 * @since 1.0
 */
public record AddInboundItemRequest(
        @NotNull Long productId,
        @NotBlank String lotNumber,
        LocalDate expiryDate,
        @NotNull @Positive Integer quantity,
        @NotNull Long locationId
) {
}
