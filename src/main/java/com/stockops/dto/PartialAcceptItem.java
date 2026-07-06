package com.stockops.dto;

/**
 * Partial acceptance request for a purchase order item.
 * Used when ERP can only fulfill a portion of the requested quantity.
 * Quantity bounds (null/negative/over-requested) are enforced by
 * {@code PurchaseOrderService.partialAccept}.
 *
 * @param poItemId purchase order item identifier
 * @param acceptedQuantity quantity accepted by ERP for this item
 * @author StockOps Team
 * @since 2.0
 */
public record PartialAcceptItem(
        Long poItemId,
        Integer acceptedQuantity
) {
}
