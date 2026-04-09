package com.stockops.dto;

import java.time.LocalDate;

/**
 * Outbound creation request payload.
 *
 * @param outboundDate requested outbound business date
 * @param customer customer name
 * @author StockOps Team
 * @since 1.0
 */
public record CreateOutboundRequest(
        LocalDate outboundDate,
        String customer) {
}
