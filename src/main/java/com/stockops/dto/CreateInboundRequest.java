package com.stockops.dto;

import java.time.LocalDate;

/**
 * Inbound header creation request payload.
 *
 * @param inboundDate requested inbound date, defaults to today when omitted
 * @param supplier supplier name
 * @author StockOps Team
 * @since 1.0
 */
public record CreateInboundRequest(
        LocalDate inboundDate,
        String supplier
) {
}
