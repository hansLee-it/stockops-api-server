package com.stockops.notification.escalation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for EscalationPolicy entities with scope-based lookups.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Repository
public interface EscalationPolicyRepository extends JpaRepository<EscalationPolicy, Long> {

    List<EscalationPolicy> findByCenterIdAndActiveTrue(Long centerId);

    Optional<EscalationPolicy> findByCenterIdAndWarehouseIdAndAlertTypeAndActiveTrue(
            Long centerId, Long warehouseId, String alertType);

    Optional<EscalationPolicy> findByCenterIdAndWarehouseIdIsNullAndAlertTypeAndActiveTrue(
            Long centerId, String alertType);

    @Query("SELECT p FROM EscalationPolicy p LEFT JOIN FETCH p.rules " +
            "WHERE p.centerId = :centerId AND p.active = true ORDER BY p.warehouseId ASC")
    List<EscalationPolicy> findAllWithRulesByCenterId(@Param("centerId") Long centerId);

    @Query("SELECT p FROM EscalationPolicy p LEFT JOIN FETCH p.rules WHERE p.id = :id")
    Optional<EscalationPolicy> findByIdWithRules(@Param("id") Long id);
}