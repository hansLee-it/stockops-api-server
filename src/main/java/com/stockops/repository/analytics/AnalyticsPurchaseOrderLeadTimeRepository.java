package com.stockops.repository.analytics;

import com.stockops.entity.analytics.AnalyticsPurchaseOrderLeadTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted analytics purchase-order lead-time rows.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AnalyticsPurchaseOrderLeadTimeRepository extends JpaRepository<AnalyticsPurchaseOrderLeadTime, Long> {

    void deleteByBusinessDateBetween(LocalDate from, LocalDate to);

    List<AnalyticsPurchaseOrderLeadTime> findByBusinessDateBetweenOrderByBusinessDateAsc(LocalDate from, LocalDate to);

    Optional<AnalyticsPurchaseOrderLeadTime> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);
}
