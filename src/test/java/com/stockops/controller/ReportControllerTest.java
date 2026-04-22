package com.stockops.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.stockops.dto.analytics.AnalyticsQueryFilter;
import com.stockops.service.AnalyticsExcelExportService;
import com.stockops.service.PdfReportService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link ReportController} analytics export responses.
 *
 * @author StockOps Team
 * @since 2.0
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private PdfReportService pdfReportService;

    @Mock
    private AnalyticsExcelExportService analyticsExcelExportService;

    @InjectMocks
    private ReportController reportController;

    /**
     * Verifies that analytics PDF exports keep the PDF response contract.
     */
    @Test
    void downloadStockAgingPdfReturnsPdfHeaders() {
        when(pdfReportService.generateStockAgingAnalyticsReport(new AnalyticsQueryFilter(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                1L,
                10L))).thenReturn(new byte[]{1, 2, 3});

        final var response = reportController.downloadStockAgingPdf(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                1L,
                10L);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("stock-aging-report.pdf");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }

    /**
     * Verifies that analytics XLSX exports return the expected spreadsheet content type.
     */
    @Test
    void downloadFillRateXlsxReturnsWorkbookHeaders() {
        when(analyticsExcelExportService.generateFillRateReport(new AnalyticsQueryFilter(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                1L,
                10L))).thenReturn(new byte[]{4, 5, 6});

        final var response = reportController.downloadFillRateXlsx(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                1L,
                10L);

        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("fill-rate-report.xlsx");
        assertThat(response.getBody()).containsExactly(4, 5, 6);
    }
}
