package com.stockops.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Inbound header response payload.
 *
 * @param id inbound identifier
 * @param inboundDate inbound registration date
 * @param supplier supplier name
 * @param status inbound status
 * @param totalQuantity total quantity across inbound items
 * @param createdBy creator user id
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record InboundDTO(
        Long id,
        LocalDate inboundDate,
        String supplier,
        String status,
        int totalQuantity,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
