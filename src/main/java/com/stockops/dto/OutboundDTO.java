package com.stockops.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Outbound header response payload.
 *
 * @param id outbound id
 * @param outboundDate outbound business date
 * @param customer customer name
 * @param status outbound status
 * @param totalQuantity total requested quantity
 * @param createdBy creator user id
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record OutboundDTO(
        Long id,
        LocalDate outboundDate,
        String customer,
        String status,
        int totalQuantity,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
