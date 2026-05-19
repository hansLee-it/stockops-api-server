package com.stockops.controller;

import com.stockops.dto.ExpiryAlertDTO;
import com.stockops.entity.ExpiryAlert;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.ExpiryAlertRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.ProductRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expiry alert API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class ExpiryAlertController {

    private final ExpiryAlertRepository expiryAlertRepository;
    private final LotRepository lotRepository;
    private final ProductRepository productRepository;

    /**
     * Returns expiry alerts.
     *
     * @param level optional alert level filter
     * @param includeAcknowledged whether acknowledged alerts should be included
     * @return expiry alert list
     */
    @GetMapping("/expiry")
    @PreAuthorize("@permissionChecker.hasPermission('EXPIRY_ALERT_READ')")
    public ResponseEntity<List<ExpiryAlertDTO>> getExpiryAlerts(
            @RequestParam(required = false) final String level,
            @RequestParam(required = false, defaultValue = "false") final boolean includeAcknowledged) {

        final List<ExpiryAlert> alerts;

        if (level != null) {
            alerts = expiryAlertRepository.findByAlertLevelAndAcknowledgedFalse(level);
        } else if (!includeAcknowledged) {
            alerts = expiryAlertRepository.findByAcknowledgedFalse();
        } else {
            alerts = expiryAlertRepository.findAll();
        }

        return ResponseEntity.ok(alerts.stream().map(this::toDTO).toList());
    }

    /**
     * Marks an expiry alert as acknowledged.
     *
     * @param id alert identifier
     * @return empty success response
     */
    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("@permissionChecker.hasPermission('EXPIRY_ALERT_MANAGE')")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable final Long id) {
        final ExpiryAlert alert = expiryAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        alert.setAcknowledged(true);
        expiryAlertRepository.save(alert);

        return ResponseEntity.ok().build();
    }

    /**
     * Returns the dashboard summary for active expiry alerts.
     *
     * @return alert counts by severity
     */
    @GetMapping("/expiry/summary")
    @PreAuthorize("@permissionChecker.hasPermission('EXPIRY_ALERT_READ')")
    public ResponseEntity<Map<String, Object>> getExpiryAlertSummary() {
        final List<ExpiryAlert> allAlerts = expiryAlertRepository.findByAcknowledgedFalse();

        final long criticalCount = allAlerts.stream().filter(a -> "CRITICAL".equals(a.getAlertLevel())).count();
        final long warningCount = allAlerts.stream().filter(a -> "WARNING".equals(a.getAlertLevel())).count();
        final long noticeCount = allAlerts.stream().filter(a -> "NOTICE".equals(a.getAlertLevel())).count();
        final long infoCount = allAlerts.stream().filter(a -> "INFO".equals(a.getAlertLevel())).count();

        final Map<String, Object> summary = new HashMap<>();
        summary.put("total", allAlerts.size());
        summary.put("critical", criticalCount);
        summary.put("warning", warningCount);
        summary.put("notice", noticeCount);
        summary.put("info", infoCount);

        return ResponseEntity.ok(summary);
    }

    private ExpiryAlertDTO toDTO(final ExpiryAlert alert) {
        final var lot = lotRepository.findById(alert.getLotId()).orElse(null);
        final var product = productRepository.findById(alert.getProductId()).orElse(null);

        return new ExpiryAlertDTO(
                alert.getId(),
                alert.getLotId(),
                lot != null ? lot.getLotNumber() : null,
                alert.getProductId(),
                product != null ? product.getName() : null,
                product != null ? product.getBarcode() : null,
                alert.getDaysUntilExpiry(),
                alert.getAlertLevel(),
                alert.getExpiryDate(),
                alert.getQuantity(),
                alert.isAcknowledged(),
                alert.getCreatedAt());
    }

    public ExpiryAlertController(final ExpiryAlertRepository expiryAlertRepository, final LotRepository lotRepository, final ProductRepository productRepository) {
        this.expiryAlertRepository = expiryAlertRepository;
        this.lotRepository = lotRepository;
        this.productRepository = productRepository;
    }
}
