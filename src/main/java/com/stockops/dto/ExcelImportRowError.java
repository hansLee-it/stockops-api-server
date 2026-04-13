package com.stockops.dto;

/**
 * Row-level validation or processing error returned from Excel batch import.
 *
 * @param rowNumber 1-based Excel row number
 * @param entityKey logical grouping key such as barcode or import reference
 * @param message error details for the row
 * @author StockOps Team
 * @since 1.0
 */
public record ExcelImportRowError(
        int rowNumber,
        String entityKey,
        String message
) {
}
