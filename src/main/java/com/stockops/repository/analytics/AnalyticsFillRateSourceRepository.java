package com.stockops.repository.analytics;

import com.stockops.entity.analytics.AnalyticsFillRateSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted analytics fill-rate source rows.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AnalyticsFillRateSourceRepository extends JpaRepository<AnalyticsFillRateSource, Long> {

    void deleteByBusinessDateBetween(LocalDate from, LocalDate to);

    List<AnalyticsFillRateSource> findByBusinessDateBetweenOrderByBusinessDateAsc(LocalDate from, LocalDate to);

    Optional<AnalyticsFillRateSource> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);
}
