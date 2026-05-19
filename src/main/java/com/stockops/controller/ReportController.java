package com.stockops.controller;

import com.stockops.dto.InventoryReportFilter;
import com.stockops.dto.TransactionReportFilter;
import com.stockops.dto.analytics.AnalyticsQueryFilter;
import com.stockops.service.PdfReportService;
import com.stockops.service.AnalyticsExcelExportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report export API controller.
 * Streams server-generated PDF reports for inventory and stock movement pages.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PdfReportService pdfReportService;
    private final AnalyticsExcelExportService analyticsExcelExportService;

    /**
     * Downloads an inventory snapshot PDF.
     *
     * @param search optional free-text search filter
     * @param status optional inventory status filter
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return PDF download response
     */
    @GetMapping(value = "/inventory/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadInventoryReport(
            @RequestParam(required = false) final String search,
            @RequestParam(required = false) final String status,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        final byte[] pdf = pdfReportService.generateInventoryReport(
                new InventoryReportFilter(search, status, centerId, warehouseId));
        return buildPdfResponse(pdf, "inventory-report.pdf");
    }

    /**
     * Downloads an inbound transaction PDF.
     *
     * @param startDate optional inclusive date range start
     * @param endDate optional inclusive date range end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @param status optional page status filter
     * @return PDF download response
     */
    @GetMapping(value = "/inbounds/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_READ')")
    public ResponseEntity<byte[]> downloadInboundReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId,
            @RequestParam(required = false) final String status) {
        final byte[] pdf = pdfReportService.generateInboundTransactionReport(
                new TransactionReportFilter(startDate, endDate, centerId, warehouseId, status));
        return buildPdfResponse(pdf, "inbound-transactions-report.pdf");
    }

    /**
     * Downloads an outbound transaction PDF.
     *
     * @param startDate optional inclusive date range start
     * @param endDate optional inclusive date range end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @param status optional page status filter
     * @return PDF download response
     */
    @GetMapping(value = "/outbounds/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_READ')")
    public ResponseEntity<byte[]> downloadOutboundReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId,
            @RequestParam(required = false) final String status) {
        final byte[] pdf = pdfReportService.generateOutboundTransactionReport(
                new TransactionReportFilter(startDate, endDate, centerId, warehouseId, status));
        return buildPdfResponse(pdf, "outbound-transactions-report.pdf");
    }

    /**
     * Downloads a stock-aging analytics PDF.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return PDF download response
     */
    @GetMapping(value = "/analytics/stock-aging/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadStockAgingPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildPdfResponse(
                pdfReportService.generateStockAgingAnalyticsReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "stock-aging-report.pdf");
    }

    /**
     * Downloads a stockout-rate analytics PDF.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return PDF download response
     */
    @GetMapping(value = "/analytics/stockout-rate/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadStockoutRatePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildPdfResponse(
                pdfReportService.generateStockoutRateAnalyticsReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "stockout-rate-report.pdf");
    }

    /**
     * Downloads an expiry-waste analytics PDF.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return PDF download response
     */
    @GetMapping(value = "/analytics/expiry-waste/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadExpiryWastePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildPdfResponse(
                pdfReportService.generateExpiryWasteAnalyticsReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "expiry-waste-report.pdf");
    }

    /**
     * Downloads a purchase-order lead-time analytics PDF.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return PDF download response
     */
    @GetMapping(value = "/analytics/purchase-order-lead-time/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'PURCHASE_ORDER_READ')")
    public ResponseEntity<byte[]> downloadPurchaseOrderLeadTimePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildPdfResponse(
                pdfReportService.generatePurchaseOrderLeadTimeAnalyticsReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "purchase-order-lead-time-report.pdf");
    }

    /**
     * Downloads a fill-rate analytics PDF.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return PDF download response
     */
    @GetMapping(value = "/analytics/fill-rate/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'PURCHASE_ORDER_READ')")
    public ResponseEntity<byte[]> downloadFillRatePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildPdfResponse(
                pdfReportService.generateFillRateAnalyticsReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "fill-rate-report.pdf");
    }

    /**
     * Downloads a stock-aging analytics XLSX workbook.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return XLSX download response
     */
    @GetMapping(value = "/analytics/stock-aging/xlsx", produces = XLSX_CONTENT_TYPE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadStockAgingXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildXlsxResponse(
                analyticsExcelExportService.generateStockAgingReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "stock-aging-report.xlsx");
    }

    /**
     * Downloads a stockout-rate analytics XLSX workbook.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return XLSX download response
     */
    @GetMapping(value = "/analytics/stockout-rate/xlsx", produces = XLSX_CONTENT_TYPE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadStockoutRateXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildXlsxResponse(
                analyticsExcelExportService.generateStockoutRateReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "stockout-rate-report.xlsx");
    }

    /**
     * Downloads an expiry-waste analytics XLSX workbook.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return XLSX download response
     */
    @GetMapping(value = "/analytics/expiry-waste/xlsx", produces = XLSX_CONTENT_TYPE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<byte[]> downloadExpiryWasteXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildXlsxResponse(
                analyticsExcelExportService.generateExpiryWasteReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "expiry-waste-report.xlsx");
    }

    /**
     * Downloads a purchase-order lead-time analytics XLSX workbook.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return XLSX download response
     */
    @GetMapping(value = "/analytics/purchase-order-lead-time/xlsx", produces = XLSX_CONTENT_TYPE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'PURCHASE_ORDER_READ')")
    public ResponseEntity<byte[]> downloadPurchaseOrderLeadTimeXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildXlsxResponse(
                analyticsExcelExportService.generatePurchaseOrderLeadTimeReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "purchase-order-lead-time-report.xlsx");
    }

    /**
     * Downloads a fill-rate analytics XLSX workbook.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @return XLSX download response
     */
    @GetMapping(value = "/analytics/fill-rate/xlsx", produces = XLSX_CONTENT_TYPE)
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'PURCHASE_ORDER_READ')")
    public ResponseEntity<byte[]> downloadFillRateXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return buildXlsxResponse(
                analyticsExcelExportService.generateFillRateReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)),
                "fill-rate-report.xlsx");
    }

    private ResponseEntity<byte[]> buildPdfResponse(final byte[] pdf, final String fileName) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private ResponseEntity<byte[]> buildXlsxResponse(final byte[] workbook, final String fileName) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(workbook.length);
        return ResponseEntity.ok().headers(headers).body(workbook);
    }

    public ReportController(final PdfReportService pdfReportService, final AnalyticsExcelExportService analyticsExcelExportService) {
        this.pdfReportService = pdfReportService;
        this.analyticsExcelExportService = analyticsExcelExportService;
    }
}
