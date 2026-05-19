package com.stockops.controller;

import com.stockops.dto.analytics.AnalyticsQueryFilter;
import com.stockops.dto.analytics.ExpiryWasteReportResponse;
import com.stockops.dto.analytics.FillRateReportResponse;
import com.stockops.dto.analytics.PurchaseOrderLeadTimeReportResponse;
import com.stockops.dto.analytics.StockAgingReportResponse;
import com.stockops.dto.analytics.StockoutRateReportResponse;
import com.stockops.service.analytics.AnalyticsReportingService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics BI API controller.
 * Exposes scoped DTO-based reporting metrics backed by the analytics read model.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsReportingService analyticsReportingService;

    /**
     * Returns stock-aging analytics.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center identifier
     * @param warehouseId optional warehouse identifier
     * @return stock-aging response
     */
    @GetMapping("/stock-aging")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<StockAgingReportResponse> getStockAging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return ResponseEntity.ok(analyticsReportingService.getStockAgingReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)));
    }

    /**
     * Returns stockout-rate analytics.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center identifier
     * @param warehouseId optional warehouse identifier
     * @return stockout-rate response
     */
    @GetMapping("/stockout-rate")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<StockoutRateReportResponse> getStockoutRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return ResponseEntity.ok(analyticsReportingService.getStockoutRateReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)));
    }

    /**
     * Returns expiry-waste analytics.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center identifier
     * @param warehouseId optional warehouse identifier
     * @return expiry-waste response
     */
    @GetMapping("/expiry-waste")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<ExpiryWasteReportResponse> getExpiryWaste(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return ResponseEntity.ok(analyticsReportingService.getExpiryWasteReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)));
    }

    /**
     * Returns purchase-order lead-time analytics.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center identifier
     * @param warehouseId optional warehouse identifier
     * @return lead-time response
     */
    @GetMapping("/purchase-order-lead-time")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'PURCHASE_ORDER_READ')")
    public ResponseEntity<PurchaseOrderLeadTimeReportResponse> getPurchaseOrderLeadTime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return ResponseEntity.ok(analyticsReportingService.getPurchaseOrderLeadTimeReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)));
    }

    /**
     * Returns fill-rate analytics.
     *
     * @param from inclusive business-date start
     * @param to inclusive business-date end
     * @param centerId optional center identifier
     * @param warehouseId optional warehouse identifier
     * @return fill-rate response
     */
    @GetMapping("/fill-rate")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'PURCHASE_ORDER_READ')")
    public ResponseEntity<FillRateReportResponse> getFillRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId) {
        return ResponseEntity.ok(analyticsReportingService.getFillRateReport(new AnalyticsQueryFilter(from, to, centerId, warehouseId)));
    }

    public AnalyticsController(final AnalyticsReportingService analyticsReportingService) {
        this.analyticsReportingService = analyticsReportingService;
    }

}
