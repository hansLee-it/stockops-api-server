package com.stockops.service;

import com.stockops.config.MetricsConfig;
import com.stockops.dto.DashboardSummaryDTO;
import com.stockops.entity.CycleCountStatus;
import com.stockops.repository.CycleCountRepository;
import com.stockops.repository.ExpiryAlertRepository;
import com.stockops.repository.InboundRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.InventoryTransactionRepository;
import com.stockops.repository.OutboundRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.StockAdjustmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard aggregation service.
 *
 * @author StockOps Team
 * @since 1.0
 * @see ProductRepository
 * @see InventoryRepository
 * @see CycleCountRepository
 * @see InboundRepository
 * @see OutboundRepository
 * @see StockAdjustmentRepository
 * @see ExpiryAlertRepository
 * @see InventoryTransactionRepository
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CycleCountRepository cycleCountRepository;
    private final InboundRepository inboundRepository;
    private final OutboundRepository outboundRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ExpiryAlertRepository expiryAlertRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MetricsConfig metricsConfig;

    /**
     * Builds the dashboard summary.
     * Cached for 60 seconds; evicted on any stock mutation.
     *
     * @return dashboard summary DTO
     */
    @Cacheable(value = "dashboard::summary")
    public DashboardSummaryDTO getSummary() {
        final io.micrometer.core.instrument.Timer.Sample sample = metricsConfig.startDashboardTimer();
        try {
            return new DashboardSummaryDTO(
                    calculateTotalProducts(),
                    calculateTotalInventoryQuantity(),
                    calculateTodayInboundCount(),
                    calculateTodayOutboundCount(),
                    calculateLowStockCount(),
                    calculatePendingCycleCounts(),
                    calculateCriticalExpiryCount(),
                    calculateWarningExpiryCount(),
                    calculateRecentTransactionCount());
        } finally {
            metricsConfig.recordDashboardDuration(sample);
        }
    }

    /**
     * Counts registered products.
     *
     * @return product count
     */
    public long calculateTotalProducts() {
        return productRepository.count();
    }

    /**
     * Sums available inventory quantity across all rows.
     *
     * @return total available quantity
     */
    public long calculateTotalInventoryQuantity() {
        return inventoryRepository.sumInventoryQuantity();
    }

    /**
     * Counts inbound headers created today.
     *
     * @return today's inbound count
     */
    public long calculateTodayInboundCount() {
        return inboundRepository.countByInboundDate(LocalDate.now());
    }

    /**
     * Counts outbound headers created today.
     *
     * @return today's outbound count
     */
    public long calculateTodayOutboundCount() {
        return outboundRepository.countByOutboundDate(LocalDate.now());
    }

    /**
     * Counts inventory rows whose available quantity is below the dashboard threshold.
     *
     * @return low-stock row count
     */
    public long calculateLowStockCount() {
        return inventoryRepository.countLowStockItems(LOW_STOCK_THRESHOLD);
    }

    /**
     * Counts cycle counts that are not yet completed or cancelled.
     *
     * @return pending cycle count total
     */
    public long calculatePendingCycleCounts() {
        return cycleCountRepository.countByStatusIn(List.of(CycleCountStatus.PENDING, CycleCountStatus.IN_PROGRESS));
    }

    /**
     * Counts active critical expiry alerts.
     *
     * @return critical expiry alert count
     */
    public long calculateCriticalExpiryCount() {
        return expiryAlertRepository.countByAlertLevelAndAcknowledgedFalse("CRITICAL");
    }

    /**
     * Counts active warning expiry alerts.
     *
     * @return warning expiry alert count
     */
    public long calculateWarningExpiryCount() {
        return expiryAlertRepository.countByAlertLevelAndAcknowledgedFalse("WARNING");
    }

    /**
     * Counts transactions created in the last 7 days.
     *
     * @return recent transaction count
     */
    public long calculateRecentTransactionCount() {
        final Instant now = Instant.now();
        final Instant start = now.minus(7, ChronoUnit.DAYS);
        return inventoryTransactionRepository.countByCreatedAtBetween(start, now);
    }

    public DashboardService(final ProductRepository productRepository, final InventoryRepository inventoryRepository, final CycleCountRepository cycleCountRepository, final InboundRepository inboundRepository, final OutboundRepository outboundRepository, final StockAdjustmentRepository stockAdjustmentRepository, final ExpiryAlertRepository expiryAlertRepository, final InventoryTransactionRepository inventoryTransactionRepository, final MetricsConfig metricsConfig) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.cycleCountRepository = cycleCountRepository;
        this.inboundRepository = inboundRepository;
        this.outboundRepository = outboundRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.expiryAlertRepository = expiryAlertRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.metricsConfig = metricsConfig;
    }
}
