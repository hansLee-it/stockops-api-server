package com.stockops.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Expiry alert response payload.
 *
 * @param id alert identifier
 * @param lotId lot identifier
 * @param lotNumber lot number
 * @param productId product identifier
 * @param productName product name
 * @param productBarcode product barcode
 * @param daysUntilExpiry days remaining until expiry
 * @param alertLevel alert severity level
 * @param expiryDate expiry date
 * @param quantity affected quantity
 * @param acknowledged whether the alert has been acknowledged
 * @param createdAt creation timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record ExpiryAlertDTO(
        Long id,
        Long lotId,
        String lotNumber,
        Long productId,
        String productName,
        String productBarcode,
        int daysUntilExpiry,
        String alertLevel,
        LocalDate expiryDate,
        int quantity,
        boolean acknowledged,
        Instant createdAt) {
}
