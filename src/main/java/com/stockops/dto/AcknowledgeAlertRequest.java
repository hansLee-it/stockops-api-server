package com.stockops.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for acknowledging an environment alert with an administrator handling note.
 *
 * @param note administrator handling/action note (optional, max 1000 chars)
 * @author StockOps Team
 * @since 1.0
 */
public record AcknowledgeAlertRequest(
        @Size(max = 1000, message = "처리내용은 1000자를 넘을 수 없습니다.")
        String note) {
}
