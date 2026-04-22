package com.stockops.repository.analytics;

import com.stockops.entity.analytics.AnalyticsExpiryWaste;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted analytics expiry-waste rows.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AnalyticsExpiryWasteRepository extends JpaRepository<AnalyticsExpiryWaste, Long> {

    void deleteByBusinessDateBetween(LocalDate from, LocalDate to);

    List<AnalyticsExpiryWaste> findByBusinessDateBetweenOrderByBusinessDateAsc(LocalDate from, LocalDate to);

    Optional<AnalyticsExpiryWaste> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);
}
