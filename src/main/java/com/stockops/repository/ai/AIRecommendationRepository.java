package com.stockops.repository.ai;

import com.stockops.entity.ai.AIRecommendation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted AI reorder recommendations.
 *
 * @author StockOps Team
 * @since 2.0
 */
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {

    @EntityGraph(attributePaths = {"forecastSnapshot", "approvedPurchaseOrder", "approvedBy"})
    List<AIRecommendation> findByBusinessDateOrderByRecommendedQuantityDescIdAsc(LocalDate businessDate);

    @EntityGraph(attributePaths = {"forecastSnapshot", "approvedPurchaseOrder", "approvedBy"})
    Optional<AIRecommendation> findById(Long id);

    List<AIRecommendation> findByBusinessDate(LocalDate businessDate);

    Optional<AIRecommendation> findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId);
}
