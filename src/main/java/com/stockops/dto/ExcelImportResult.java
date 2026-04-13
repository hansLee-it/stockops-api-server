package com.stockops.dto;

import java.util.List;

/**
 * Batch import summary for an uploaded Excel workbook.
 *
 * @param entityType imported entity type
 * @param totalRows number of non-empty data rows inspected
 * @param successCount number of rows imported successfully
 * @param failureCount number of rows rejected
 * @param errors row-level validation and processing errors
 * @author StockOps Team
 * @since 1.0
 */
public record ExcelImportResult(
        ExcelEntityType entityType,
        int totalRows,
        int successCount,
        int failureCount,
        List<ExcelImportRowError> errors
) {
}
