package com.stockops.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.stockops.dto.analytics.AnalyticsQueryFilter;
import com.stockops.dto.analytics.ExpiryWasteReportResponse;
import com.stockops.dto.analytics.FillRateReportResponse;
import com.stockops.dto.analytics.PurchaseOrderLeadTimeReportResponse;
import com.stockops.dto.analytics.StockAgingReportResponse;
import com.stockops.dto.analytics.StockoutRateReportResponse;
import com.stockops.entity.Center;
import com.stockops.entity.Product;
import com.stockops.entity.Warehouse;
import com.stockops.entity.analytics.AnalyticsDemandHistory;
import com.stockops.entity.analytics.AnalyticsExpiryWaste;
import com.stockops.entity.analytics.AnalyticsFillRateSource;
import com.stockops.entity.analytics.AnalyticsPurchaseOrderLeadTime;
import com.stockops.entity.analytics.AnalyticsStockPosition;
import com.stockops.exception.ForbiddenException;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.repository.analytics.AnalyticsDemandHistoryRepository;
import com.stockops.repository.analytics.AnalyticsExpiryWasteRepository;
import com.stockops.repository.analytics.AnalyticsFillRateSourceRepository;
import com.stockops.repository.analytics.AnalyticsPurchaseOrderLeadTimeRepository;
import com.stockops.repository.analytics.AnalyticsStockPositionRepository;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAssignment;
import com.stockops.security.ScopeType;
import com.stockops.security.ScopedUserDetails;
import com.stockops.service.AnalyticsExcelExportService;
import com.stockops.service.PdfReportService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for scoped analytics reporting and exports.
 *
 * @author StockOps Team
 * @since 2.0
 */
@SpringBootTest
@Transactional
class AnalyticsReportingServiceIntegrationTest {

    @Autowired
    private AnalyticsReportingService analyticsReportingService;

    @Autowired
    private AnalyticsExcelExportService analyticsExcelExportService;

    @Autowired
    private PdfReportService pdfReportService;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AnalyticsDemandHistoryRepository analyticsDemandHistoryRepository;

    @Autowired
    private AnalyticsStockPositionRepository analyticsStockPositionRepository;

    @Autowired
    private AnalyticsExpiryWasteRepository analyticsExpiryWasteRepository;

    @Autowired
    private AnalyticsPurchaseOrderLeadTimeRepository analyticsPurchaseOrderLeadTimeRepository;

    @Autowired
    private AnalyticsFillRateSourceRepository analyticsFillRateSourceRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifies that all five analytics metrics return scoped DTOs and that PDF/XLSX exports stay aligned with shared totals.
     *
     * @throws IOException when workbook parsing fails during verification
     */
    @Test
    void analyticsMetricsAndExportsStayScopedAndAligned() throws IOException {
        final SeedContext seed = seedAnalyticsContext();
        authenticateGlobalReportingUser();

        final AnalyticsQueryFilter filter = new AnalyticsQueryFilter(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                seed.center().getId(),
                seed.primaryWarehouse().getId());

        final StockAgingReportResponse stockAging = analyticsReportingService.getStockAgingReport(filter);
        final StockoutRateReportResponse stockoutRate = analyticsReportingService.getStockoutRateReport(filter);
        final ExpiryWasteReportResponse expiryWaste = analyticsReportingService.getExpiryWasteReport(filter);
        final PurchaseOrderLeadTimeReportResponse leadTime = analyticsReportingService.getPurchaseOrderLeadTimeReport(filter);
        final FillRateReportResponse fillRate = analyticsReportingService.getFillRateReport(filter);

        assertThat(stockAging.rows()).hasSize(1);
        assertThat(stockAging.summary().totalAvailableQuantity()).isEqualTo(42);
        assertThat(stockoutRate.rows()).hasSize(1);
        assertThat(stockoutRate.summary().stockoutDayCount()).isEqualTo(1);
        assertThat(expiryWaste.rows()).hasSize(1);
        assertThat(expiryWaste.summary().quarantinedQuantity()).isEqualTo(5);
        assertThat(leadTime.rows()).hasSize(1);
        assertThat(leadTime.summary().averageLeadTimeHours()).isEqualByComparingTo("48");
        assertThat(fillRate.rows()).hasSize(1);
        assertThat(fillRate.summary().requestedQuantity()).isEqualTo(20);
        assertThat(fillRate.summary().shippedQuantity()).isEqualTo(12);

        final byte[] stockAgingPdf = pdfReportService.generateStockAgingAnalyticsReport(filter);
        final String pdfText = extractPdfText(stockAgingPdf);
        assertThat(pdfText).contains("Total Available Qty: 42");
        assertThat(pdfText).contains("Rows: 1");

        final byte[] fillRateWorkbook = analyticsExcelExportService.generateFillRateReport(filter);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fillRateWorkbook))) {
            final Sheet summarySheet = workbook.getSheet("summary");
            assertThat(readSummaryValue(summarySheet, "Requested Qty")).isEqualTo("20");
            assertThat(readSummaryValue(summarySheet, "Shipped Qty")).isEqualTo("12");
            assertThat(readSummaryValue(summarySheet, "Acceptance Rate")).isEqualTo("80%");
            assertThat(readSummaryValue(summarySheet, "Shipped Fill Rate")).isEqualTo("60%");
        }
    }

    /**
     * Verifies that a warehouse-scoped user cannot export analytics outside the assigned warehouse scope.
     */
    @Test
    void outOfScopeAnalyticsExportIsDenied() {
        final SeedContext seed = seedAnalyticsContext();
        authenticateWarehouseScopedReportingUser(seed.primaryWarehouse().getId());

        assertThatThrownBy(() -> analyticsExcelExportService.generateFillRateReport(new AnalyticsQueryFilter(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                seed.center().getId(),
                seed.secondaryWarehouse().getId())))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied for warehouse: " + seed.secondaryWarehouse().getId());
    }

    private SeedContext seedAnalyticsContext() {
        final Center center = new Center();
        center.setCode("CENTER-A");
        center.setName("Center A");
        final Center savedCenter = centerRepository.save(center);

        final Warehouse primaryWarehouse = new Warehouse();
        primaryWarehouse.setCenter(savedCenter);
        primaryWarehouse.setCode("WH-10");
        primaryWarehouse.setName("Warehouse 10");
        final Warehouse savedPrimaryWarehouse = warehouseRepository.save(primaryWarehouse);

        final Warehouse secondaryWarehouse = new Warehouse();
        secondaryWarehouse.setCenter(savedCenter);
        secondaryWarehouse.setCode("WH-11");
        secondaryWarehouse.setName("Warehouse 11");
        final Warehouse savedSecondaryWarehouse = warehouseRepository.save(secondaryWarehouse);

        final Product product = new Product();
        product.setBarcode("P-1001");
        product.setName("Analytics Product");
        product.setUnit("EA");
        product.setExpiryManaged(true);
        product.setDefaultPrice(BigDecimal.ONE);
        final Product savedProduct = productRepository.save(product);

        final Product otherProduct = new Product();
        otherProduct.setBarcode("P-2002");
        otherProduct.setName("Out Of Scope Product");
        otherProduct.setUnit("EA");
        otherProduct.setExpiryManaged(true);
        otherProduct.setDefaultPrice(BigDecimal.ONE);
        final Product savedOtherProduct = productRepository.save(otherProduct);

        saveDemand(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 10), 6, 1, false);
        saveDemand(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 20), 6, 1, false);
        saveStockPosition(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 1), 10, 10, 0, 0);
        saveStockPosition(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 15), 0, 0, 0, 0);
        saveStockPosition(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 30), 42, 42, 0, 0);
        saveExpiryWaste(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 18), 5, 2);
        saveLeadTime(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 18), 1, 1, 48);
        saveFillRate(savedProduct.getId(), savedCenter.getId(), savedPrimaryWarehouse.getId(), LocalDate.of(2026, 4, 18), 1, 20, 16, 4, 12);

        saveDemand(savedOtherProduct.getId(), savedCenter.getId(), savedSecondaryWarehouse.getId(), LocalDate.of(2026, 4, 12), 4, 1, false);
        saveStockPosition(savedOtherProduct.getId(), savedCenter.getId(), savedSecondaryWarehouse.getId(), LocalDate.of(2026, 4, 30), 99, 99, 0, 0);
        saveFillRate(savedOtherProduct.getId(), savedCenter.getId(), savedSecondaryWarehouse.getId(), LocalDate.of(2026, 4, 18), 1, 50, 50, 0, 50);

        return new SeedContext(savedCenter, savedPrimaryWarehouse, savedSecondaryWarehouse, savedProduct);
    }

    private void saveDemand(final Long productId,
                            final Long centerId,
                            final Long warehouseId,
                            final LocalDate businessDate,
                            final int quantity,
                            final int eventCount,
                            final boolean insufficientHistory) {
        final AnalyticsDemandHistory row = new AnalyticsDemandHistory();
        row.setBusinessDate(businessDate);
        row.setProductId(productId);
        row.setCenterId(centerId);
        row.setWarehouseId(warehouseId);
        row.setConfirmedOutboundQuantity(quantity);
        row.setConfirmedOutboundEventCount(eventCount);
        row.setInsufficientHistory(insufficientHistory);
        analyticsDemandHistoryRepository.save(row);
    }

    private void saveStockPosition(final Long productId,
                                   final Long centerId,
                                   final Long warehouseId,
                                   final LocalDate businessDate,
                                   final int onHandQuantity,
                                   final int availableQuantity,
                                   final int reservedQuantity,
                                   final int quarantinedQuantity) {
        final AnalyticsStockPosition row = new AnalyticsStockPosition();
        row.setBusinessDate(businessDate);
        row.setProductId(productId);
        row.setCenterId(centerId);
        row.setWarehouseId(warehouseId);
        row.setOnHandQuantity(onHandQuantity);
        row.setAvailableQuantity(availableQuantity);
        row.setReservedQuantity(reservedQuantity);
        row.setQuarantinedQuantity(quarantinedQuantity);
        analyticsStockPositionRepository.save(row);
    }

    private void saveExpiryWaste(final Long productId,
                                 final Long centerId,
                                 final Long warehouseId,
                                 final LocalDate businessDate,
                                 final int quantity,
                                 final int lotCount) {
        final AnalyticsExpiryWaste row = new AnalyticsExpiryWaste();
        row.setBusinessDate(businessDate);
        row.setProductId(productId);
        row.setCenterId(centerId);
        row.setWarehouseId(warehouseId);
        row.setQuarantinedQuantity(quantity);
        row.setQuarantinedLotCount(lotCount);
        analyticsExpiryWasteRepository.save(row);
    }

    private void saveLeadTime(final Long productId,
                              final Long centerId,
                              final Long warehouseId,
                              final LocalDate businessDate,
                              final int purchaseOrderCount,
                              final int sampleCount,
                              final long totalLeadTimeHours) {
        final AnalyticsPurchaseOrderLeadTime row = new AnalyticsPurchaseOrderLeadTime();
        row.setBusinessDate(businessDate);
        row.setProductId(productId);
        row.setCenterId(centerId);
        row.setWarehouseId(warehouseId);
        row.setPurchaseOrderCount(purchaseOrderCount);
        row.setLeadTimeSampleCount(sampleCount);
        row.setTotalLeadTimeHours(totalLeadTimeHours);
        analyticsPurchaseOrderLeadTimeRepository.save(row);
    }

    private void saveFillRate(final Long productId,
                              final Long centerId,
                              final Long warehouseId,
                              final LocalDate businessDate,
                              final int purchaseOrderCount,
                              final int requestedQuantity,
                              final int acceptedQuantity,
                              final int cancelledQuantity,
                              final int shippedQuantity) {
        final AnalyticsFillRateSource row = new AnalyticsFillRateSource();
        row.setBusinessDate(businessDate);
        row.setProductId(productId);
        row.setCenterId(centerId);
        row.setWarehouseId(warehouseId);
        row.setPurchaseOrderCount(purchaseOrderCount);
        row.setRequestedQuantity(requestedQuantity);
        row.setAcceptedQuantity(acceptedQuantity);
        row.setCancelledQuantity(cancelledQuantity);
        row.setShippedQuantity(shippedQuantity);
        analyticsFillRateSourceRepository.save(row);
    }

    private void authenticateGlobalReportingUser() {
        final ScopeAccessProfile scope = new ScopeAccessProfile(true, List.of(ScopeAssignment.global()), Set.of(), Set.of());
        final ScopedUserDetails userDetails = new ScopedUserDetails(
                1L,
                "analytics.admin@stockops.local",
                "password",
                true,
                List.of(
                        new SimpleGrantedAuthority("DASHBOARD_READ"),
                        new SimpleGrantedAuthority("INVENTORY_READ"),
                        new SimpleGrantedAuthority("PURCHASE_ORDER_READ")),
                scope);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private void authenticateWarehouseScopedReportingUser(final Long warehouseId) {
        final ScopeAccessProfile scope = new ScopeAccessProfile(
                false,
                List.of(new ScopeAssignment(ScopeType.WAREHOUSE, null, warehouseId)),
                Set.of(),
                Set.of(warehouseId));
        final ScopedUserDetails userDetails = new ScopedUserDetails(
                2L,
                "warehouse.manager@stockops.local",
                "password",
                true,
                List.of(new SimpleGrantedAuthority("PURCHASE_ORDER_READ"), new SimpleGrantedAuthority("DASHBOARD_READ")),
                scope);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private String extractPdfText(final byte[] pdfBytes) throws IOException {
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            final StringBuilder builder = new StringBuilder();
            final PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int pageNumber = 1; pageNumber <= reader.getNumberOfPages(); pageNumber++) {
                builder.append(extractor.getTextFromPage(pageNumber));
            }
            return builder.toString();
        }
    }

    private String readSummaryValue(final Sheet sheet, final String key) {
        return java.util.stream.IntStream.rangeClosed(sheet.getFirstRowNum(), sheet.getLastRowNum())
                .mapToObj(sheet::getRow)
                .filter(row -> row != null && row.getCell(0) != null)
                .filter(row -> key.equals(row.getCell(0).getStringCellValue()))
                .map(row -> row.getCell(1).getStringCellValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Summary key not found: " + key));
    }

    private record SeedContext(Center center, Warehouse primaryWarehouse, Warehouse secondaryWarehouse, Product product) {
    }
}
