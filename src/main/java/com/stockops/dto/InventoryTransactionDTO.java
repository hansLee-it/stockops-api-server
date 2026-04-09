package com.stockops.dto;

import java.time.Instant;

/**
 * Inventory transaction response payload.
 *
 * @param id transaction identifier
 * @param type transaction type
 * @param productId product identifier
 * @param productName product name
 * @param locationId location identifier
 * @param locationCode location code
 * @param lotId lot identifier
 * @param lotNumber lot number
 * @param quantity transaction quantity
 * @param beforeQuantity quantity before transaction
 * @param afterQuantity quantity after transaction
 * @param referenceId reference identifier
 * @param referenceType reference type
 * @param createdBy operator identifier
 * @param createdAt creation timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record InventoryTransactionDTO(
        Long id,
        String type,
        Long productId,
        String productName,
        Long locationId,
        String locationCode,
        Long lotId,
        String lotNumber,
        int quantity,
        int beforeQuantity,
        int afterQuantity,
        Long referenceId,
        String referenceType,
        Long createdBy,
        Instant createdAt
) {
}
