package com.stockops.repository.ai;

import com.stockops.entity.ai.AIForecastSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for deterministic forecast snapshots.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AIForecastSnapshotRepository extends JpaRepository<AIForecastSnapshot, Long> {

    List<AIForecastSnapshot> findByBusinessDate(LocalDate businessDate);

    Optional<AIForecastSnapshot> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);
}
