package com.stockops.dto;

import java.time.Instant;

/**
 * Outbound item response payload.
 *
 * @param id outbound item id
 * @param outboundId outbound id
 * @param productId product id
 * @param productName product name
 * @param lotId lot id allocated during confirmation
 * @param lotNumber lot number allocated during confirmation
 * @param quantity allocated quantity
 * @param createdAt creation timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record OutboundItemDTO(
        Long id,
        Long outboundId,
        Long productId,
        String productName,
        Long lotId,
        String lotNumber,
        int quantity,
        Instant createdAt) {
}
