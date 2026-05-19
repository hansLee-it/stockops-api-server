package com.stockops.report;

import com.stockops.dto.InventoryTurnoverDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for inventory turnover report API.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/reports")
public class InventoryTurnoverController {

    private final InventoryTurnoverReportService reportService;

    /**
     * Returns product-wise inventory turnover rate for a given period.
     *
     * @param startDate period start (ISO date, required)
     * @param endDate period end (ISO date, required)
     * @param centerId optional center filter
     * @return list of turnover DTOs sorted by turnoverRate descending
     */
    @GetMapping("/inventory-turnover")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public List<InventoryTurnoverDTO> getInventoryTurnover(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long centerId) {

        if (centerId != null) {
            return reportService.generateReport(startDate, endDate, centerId);
        }
        return reportService.generateReport(startDate, endDate);
    }

    public InventoryTurnoverController(final InventoryTurnoverReportService reportService) {
        this.reportService = reportService;
    }

}
