package com.stockops.repository.analytics;

import com.stockops.entity.analytics.AnalyticsStockPosition;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted analytics stock-position rows.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AnalyticsStockPositionRepository extends JpaRepository<AnalyticsStockPosition, Long> {

    void deleteByBusinessDateBetween(LocalDate from, LocalDate to);

    List<AnalyticsStockPosition> findByBusinessDateBetweenOrderByBusinessDateAsc(LocalDate from, LocalDate to);

    Optional<AnalyticsStockPosition> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);
}
