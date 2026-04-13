package com.stockops.controller;

import com.stockops.dto.InventoryReportFilter;
import com.stockops.dto.TransactionReportFilter;
import com.stockops.service.PdfReportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
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

    private ResponseEntity<byte[]> buildPdfResponse(final byte[] pdf, final String fileName) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
