package com.stockops.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Outbound item creation request payload.
 *
 * @param productId product id
 * @param quantity requested outbound quantity
 * @author StockOps Team
 * @since 1.0
 */
public record AddOutboundItemRequest(
        @NotNull Long productId,
        @Positive int quantity) {
}
