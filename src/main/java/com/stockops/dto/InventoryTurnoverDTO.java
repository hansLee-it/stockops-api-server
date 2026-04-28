package com.stockops.dto;

import java.math.BigDecimal;

/**
 * Inventory turnover report data transfer object.
 * Represents product-wise turnover rate for a given period.
 *
 * @param productId product identifier
 * @param productName product display name
 * @param productCode product barcode/code
 * @param unitPrice product default price used for COGS calculation
 * @param beginningInventoryQty inventory quantity at period start
 * @param endingInventoryQty inventory quantity at period end
 * @param averageInventoryQty average of beginning and ending inventory quantities
 * @param cogs cost of goods sold (total outbound quantity × unit price)
 * @param turnoverRate annualized turnover rate, rounded to 2 decimal places; 0.00 when average inventory is zero
 * @param periodDays number of days in the reporting period
 * @author StockOps Team
 * @since 2.0
 */
public record InventoryTurnoverDTO(
        Long productId,
        String productName,
        String productCode,
        BigDecimal unitPrice,
        int beginningInventoryQty,
        int endingInventoryQty,
        int averageInventoryQty,
        BigDecimal cogs,
        BigDecimal turnoverRate,
        int periodDays
) {
}
