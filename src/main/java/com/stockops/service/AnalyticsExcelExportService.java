package com.stockops.service;

import com.stockops.dto.analytics.AnalyticsQueryFilter;
import com.stockops.dto.analytics.ExpiryWasteReportResponse;
import com.stockops.dto.analytics.FillRateReportResponse;
import com.stockops.dto.analytics.PurchaseOrderLeadTimeReportResponse;
import com.stockops.dto.analytics.StockAgingReportResponse;
import com.stockops.dto.analytics.StockoutRateReportResponse;
import com.stockops.service.analytics.AnalyticsReportingService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates analytics XLSX exports from the shared scoped analytics query service.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalyticsReportingService analyticsReportingService;

    /**
     * Generates a stock-aging XLSX export.
     *
     * @param filter scoped analytics filter
     * @return XLSX bytes
     */
    public byte[] generateStockAgingReport(final AnalyticsQueryFilter filter) {
        final StockAgingReportResponse report = analyticsReportingService.getStockAgingReport(filter);
        final Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Row Count", String.valueOf(report.summary().rowCount()));
        summary.put("Total Available Qty", String.valueOf(report.summary().totalAvailableQuantity()));
        summary.put("0-30 Days Qty", String.valueOf(report.summary().zeroToThirtyQuantity()));
        summary.put("31-60 Days Qty", String.valueOf(report.summary().thirtyOneToSixtyQuantity()));
        summary.put("61-90 Days Qty", String.valueOf(report.summary().sixtyOneToNinetyQuantity()));
        summary.put("90+ Days Qty", String.valueOf(report.summary().overNinetyQuantity()));
        summary.put("No Demand Qty", String.valueOf(report.summary().noDemandQuantity()));
        return buildWorkbook(
                "stock-aging",
                filter,
                summary,
                List.of("Product", "Center", "Warehouse", "Business Date", "Available Qty", "Avg Daily Demand", "Coverage Days", "Bucket"),
                report.rows().stream()
                        .map(row -> List.of(
                                row.productName(),
                                row.centerName(),
                                row.warehouseName(),
                                formatDate(row.businessDate()),
                                String.valueOf(row.availableQuantity()),
                                formatDecimal(row.averageDailyDemand()),
                                formatDecimal(row.estimatedCoverageDays()),
                                row.agingBucket()))
                        .toList());
    }

    /**
     * Generates a stockout-rate XLSX export.
     *
     * @param filter scoped analytics filter
     * @return XLSX bytes
     */
    public byte[] generateStockoutRateReport(final AnalyticsQueryFilter filter) {
        final StockoutRateReportResponse report = analyticsReportingService.getStockoutRateReport(filter);
        final Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Row Count", String.valueOf(report.summary().rowCount()));
        summary.put("Observed Days", String.valueOf(report.summary().observedDayCount()));
        summary.put("Stockout Days", String.valueOf(report.summary().stockoutDayCount()));
        summary.put("Overall Stockout Rate", formatRatio(report.summary().overallStockoutRate()));
        return buildWorkbook(
                "stockout-rate",
                filter,
                summary,
                List.of("Product", "Center", "Warehouse", "Observed Days", "Stockout Days", "Stockout Rate", "Latest Available Qty"),
                report.rows().stream()
                        .map(row -> List.of(
                                row.productName(),
                                row.centerName(),
                                row.warehouseName(),
                                String.valueOf(row.observedDayCount()),
                                String.valueOf(row.stockoutDayCount()),
                                formatRatio(row.stockoutRate()),
                                String.valueOf(row.latestAvailableQuantity())))
                        .toList());
    }

    /**
     * Generates an expiry-waste XLSX export.
     *
     * @param filter scoped analytics filter
     * @return XLSX bytes
     */
    public byte[] generateExpiryWasteReport(final AnalyticsQueryFilter filter) {
        final ExpiryWasteReportResponse report = analyticsReportingService.getExpiryWasteReport(filter);
        final Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Row Count", String.valueOf(report.summary().rowCount()));
        summary.put("Quarantined Qty", String.valueOf(report.summary().quarantinedQuantity()));
        summary.put("Quarantined Lot Count", String.valueOf(report.summary().quarantinedLotCount()));
        return buildWorkbook(
                "expiry-waste",
                filter,
                summary,
                List.of("Product", "Center", "Warehouse", "Quarantined Qty", "Quarantined Lot Count"),
                report.rows().stream()
                        .map(row -> List.of(
                                row.productName(),
                                row.centerName(),
                                row.warehouseName(),
                                String.valueOf(row.quarantinedQuantity()),
                                String.valueOf(row.quarantinedLotCount())))
                        .toList());
    }

    /**
     * Generates a purchase-order lead-time XLSX export.
     *
     * @param filter scoped analytics filter
     * @return XLSX bytes
     */
    public byte[] generatePurchaseOrderLeadTimeReport(final AnalyticsQueryFilter filter) {
        final PurchaseOrderLeadTimeReportResponse report = analyticsReportingService.getPurchaseOrderLeadTimeReport(filter);
        final Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Row Count", String.valueOf(report.summary().rowCount()));
        summary.put("Purchase Orders", String.valueOf(report.summary().purchaseOrderCount()));
        summary.put("Lead Time Samples", String.valueOf(report.summary().leadTimeSampleCount()));
        summary.put("Total Lead Time Hours", String.valueOf(report.summary().totalLeadTimeHours()));
        summary.put("Average Lead Time Hours", formatDecimal(report.summary().averageLeadTimeHours()));
        return buildWorkbook(
                "purchase-order-lead-time",
                filter,
                summary,
                List.of("Product", "Center", "Warehouse", "Purchase Orders", "Lead Time Samples", "Total Lead Time Hours", "Average Lead Time Hours"),
                report.rows().stream()
                        .map(row -> List.of(
                                row.productName(),
                                row.centerName(),
                                row.warehouseName(),
                                String.valueOf(row.purchaseOrderCount()),
                                String.valueOf(row.leadTimeSampleCount()),
                                String.valueOf(row.totalLeadTimeHours()),
                                formatDecimal(row.averageLeadTimeHours())))
                        .toList());
    }

    /**
     * Generates a fill-rate XLSX export.
     *
     * @param filter scoped analytics filter
     * @return XLSX bytes
     */
    public byte[] generateFillRateReport(final AnalyticsQueryFilter filter) {
        final FillRateReportResponse report = analyticsReportingService.getFillRateReport(filter);
        final Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Row Count", String.valueOf(report.summary().rowCount()));
        summary.put("Purchase Orders", String.valueOf(report.summary().purchaseOrderCount()));
        summary.put("Requested Qty", String.valueOf(report.summary().requestedQuantity()));
        summary.put("Accepted Qty", String.valueOf(report.summary().acceptedQuantity()));
        summary.put("Cancelled Qty", String.valueOf(report.summary().cancelledQuantity()));
        summary.put("Shipped Qty", String.valueOf(report.summary().shippedQuantity()));
        summary.put("Acceptance Rate", formatPercent(report.summary().acceptanceRate()));
        summary.put("Shipped Fill Rate", formatPercent(report.summary().shippedFillRate()));
        return buildWorkbook(
                "fill-rate",
                filter,
                summary,
                List.of("Product", "Center", "Warehouse", "Purchase Orders", "Requested Qty", "Accepted Qty", "Cancelled Qty", "Shipped Qty", "Acceptance Rate", "Shipped Fill Rate"),
                report.rows().stream()
                        .map(row -> List.of(
                                row.productName(),
                                row.centerName(),
                                row.warehouseName(),
                                String.valueOf(row.purchaseOrderCount()),
                                String.valueOf(row.requestedQuantity()),
                                String.valueOf(row.acceptedQuantity()),
                                String.valueOf(row.cancelledQuantity()),
                                String.valueOf(row.shippedQuantity()),
                                formatPercent(row.acceptanceRate()),
                                formatPercent(row.shippedFillRate())))
                        .toList());
    }

    private byte[] buildWorkbook(final String dataSheetName,
                                 final AnalyticsQueryFilter filter,
                                 final Map<String, String> summary,
                                 final List<String> headers,
                                 final List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeSummarySheet(workbook, filter, summary);
            writeDataSheet(workbook, dataSheetName, headers, rows);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to generate analytics XLSX export", exception);
        }
    }

    private void writeSummarySheet(final Workbook workbook,
                                   final AnalyticsQueryFilter filter,
                                   final Map<String, String> summary) {
        final Sheet sheet = workbook.createSheet("summary");
        int rowIndex = 0;
        rowIndex = writeKeyValueRow(sheet, rowIndex, "From", formatDate(filter.from()));
        rowIndex = writeKeyValueRow(sheet, rowIndex, "To", formatDate(filter.to()));
        rowIndex = writeKeyValueRow(sheet, rowIndex, "Center Id", filter.centerId() == null ? "All" : String.valueOf(filter.centerId()));
        rowIndex = writeKeyValueRow(sheet, rowIndex, "Warehouse Id", filter.warehouseId() == null ? "All" : String.valueOf(filter.warehouseId()));
        rowIndex++;
        for (final Map.Entry<String, String> entry : summary.entrySet()) {
            rowIndex = writeKeyValueRow(sheet, rowIndex, entry.getKey(), entry.getValue());
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeDataSheet(final Workbook workbook,
                                final String sheetName,
                                final List<String> headers,
                                final List<List<String>> rows) {
        final Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;
        final Row headerRow = sheet.createRow(rowIndex++);
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            headerRow.createCell(columnIndex).setCellValue(headers.get(columnIndex));
        }
        for (final List<String> rowValues : rows) {
            final Row row = sheet.createRow(rowIndex++);
            for (int columnIndex = 0; columnIndex < rowValues.size(); columnIndex++) {
                row.createCell(columnIndex).setCellValue(rowValues.get(columnIndex));
            }
        }
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }
    }

    private int writeKeyValueRow(final Sheet sheet, final int rowIndex, final String key, final String value) {
        final Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
        return rowIndex + 1;
    }

    private String formatDate(final LocalDate value) {
        return value == null ? "All" : DATE_FORMATTER.format(value);
    }

    private String formatDecimal(final BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String formatRatio(final BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String formatPercent(final BigDecimal value) {
        return value == null ? "0%" : value.stripTrailingZeros().toPlainString() + "%";
    }

    public AnalyticsExcelExportService(final AnalyticsReportingService analyticsReportingService) {
        this.analyticsReportingService = analyticsReportingService;
    }

}
