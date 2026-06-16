package com.stockops.repository.ai;

import com.stockops.entity.ai.ForecastProposalRun;
import com.stockops.entity.ai.ForecastProposalStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for intraday forecast proposal runs.
 *
 * @author StockOps Team
 * @since 2.4
 */
public interface ForecastProposalRunRepository extends JpaRepository<ForecastProposalRun, Long> {

    @EntityGraph(attributePaths = {"approvedPurchaseOrder", "approvedBy", "rejectedBy"})
    Optional<ForecastProposalRun> findById(Long id);

    /** All proposals for a business date, newest slot first then largest quantity. */
    List<ForecastProposalRun> findByBusinessDateOrderByRunSlotDescRecommendedQuantityDescIdAsc(LocalDate businessDate);

    /** All proposals for a single slot of a business date (used to refresh a slot in place). */
    List<ForecastProposalRun> findByBusinessDateAndRunSlot(LocalDate businessDate, Integer runSlot);

    /** All proposals for a business date in a given status (e.g. already-approved scopes). */
    List<ForecastProposalRun> findByBusinessDateAndStatus(LocalDate businessDate, ForecastProposalStatus status);

    /** Single proposal lookup for upsert by accumulation key. */
    Optional<ForecastProposalRun> findByBusinessDateAndRunSlotAndProductIdAndCenterIdAndWarehouseId(
            LocalDate businessDate,
            Integer runSlot,
            Long productId,
            Long centerId,
            Long warehouseId);

    /** Sibling proposals (across all slots) for a scope/day in a given status — used to supersede on approval. */
    List<ForecastProposalRun> findByBusinessDateAndProductIdAndCenterIdAndWarehouseIdAndStatus(
            LocalDate businessDate,
            Long productId,
            Long centerId,
            Long warehouseId,
            ForecastProposalStatus status);

    /** Flips open proposals past their actionable window to EXPIRED. Returns the number updated. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForecastProposalRun p "
            + "SET p.status = com.stockops.entity.ai.ForecastProposalStatus.EXPIRED, p.updatedAt = :now "
            + "WHERE p.status = com.stockops.entity.ai.ForecastProposalStatus.PROPOSED "
            + "AND p.actionableUntil < :now")
    int expireProposalsPastWindow(@Param("now") Instant now);
}
