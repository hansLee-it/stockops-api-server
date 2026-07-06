package com.stockops.dto;

import java.time.LocalDate;

/**
 * Optional body for store-originated purchase requests (client-web 발주 신청).
 * Both fields are optional so existing callers that POST without a body keep working.
 *
 * @param reason free-text request reason (사유); stored on the order's notes
 * @param expectedDate requested delivery date (희망 입고일)
 * @author StockOps Team
 * @since 2.0
 */
public record CreateStoreRequestBody(
        String reason,
        LocalDate expectedDate
) {
}
