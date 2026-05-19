package com.stockops.controller;

import com.stockops.dto.AbcClassificationDTO;
import com.stockops.dto.AbcXyzMatrixDTO;
import com.stockops.dto.XyzClassificationDTO;
import com.stockops.service.AbcXyzReportService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ABC/XYZ inventory classification API controller.
 * Exposes endpoints for ABC analysis, XYZ analysis, and the combined ABC-XYZ matrix.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/reports")
public class AbcXyzController {

    private final AbcXyzReportService abcXyzReportService;

    /**
     * Returns ABC classification for products in a center.
     *
     * @param centerId center identifier (required)
     * @return ordered list of ABC classification results
     */
    @GetMapping("/abc-analysis")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<List<AbcClassificationDTO>> getAbcAnalysis(
            @RequestParam final Long centerId) {
        return ResponseEntity.ok(abcXyzReportService.getAbcAnalysis(centerId));
    }

    /**
     * Returns XYZ classification for products in a center.
     *
     * @param centerId center identifier (required)
     * @return list of XYZ classification results
     */
    @GetMapping("/xyz-analysis")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<List<XyzClassificationDTO>> getXyzAnalysis(
            @RequestParam final Long centerId) {
        return ResponseEntity.ok(abcXyzReportService.getXyzAnalysis(centerId));
    }

    /**
     * Returns the combined ABC-XYZ classification matrix for a center.
     *
     * @param centerId center identifier (required)
     * @return 3×3 matrix with product counts and details per cell
     */
    @GetMapping("/abc-xyz-matrix")
    @PreAuthorize("@permissionChecker.hasAnyPermission('DASHBOARD_READ', 'INVENTORY_READ')")
    public ResponseEntity<AbcXyzMatrixDTO> getAbcXyzMatrix(
            @RequestParam final Long centerId) {
        return ResponseEntity.ok(abcXyzReportService.getAbcXyzMatrix(centerId));
    }

    public AbcXyzController(final AbcXyzReportService abcXyzReportService) {
        this.abcXyzReportService = abcXyzReportService;
    }

}