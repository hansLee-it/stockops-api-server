package com.stockops.repository.analytics;

import com.stockops.entity.analytics.AnalyticsDemandHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted analytics demand rows.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AnalyticsDemandHistoryRepository extends JpaRepository<AnalyticsDemandHistory, Long> {

    void deleteByBusinessDateBetween(LocalDate from, LocalDate to);

    Optional<AnalyticsDemandHistory> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);

    List<AnalyticsDemandHistory> findByBusinessDateBetweenOrderByBusinessDateAsc(LocalDate from, LocalDate to);
}
