package com.stockops.notification.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for notification channel configuration persistence.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Repository
public interface NotificationChannelConfigRepository extends JpaRepository<NotificationChannelConfig, Long> {

    /**
     * Finds all active configs for a given center.
     *
     * @param centerId center identifier
     * @return list of active channel configs for the center
     */
    List<NotificationChannelConfig> findByCenterIdAndActiveTrue(Long centerId);

    /**
     * Finds the most specific active config for a center/warehouse/alertType scope.
     * Prefers warehouse-specific config over center-level fallback.
     *
     * @param centerId    center identifier
     * @param warehouseId warehouse identifier (nullable)
     * @param alertType   alert type string
     * @return matching active config, or empty if none found
     */
    Optional<NotificationChannelConfig> findByCenterIdAndWarehouseIdAndAlertTypeAndActiveTrue(
            Long centerId, Long warehouseId, String alertType);

    /**
     * Finds center-level (warehouseId IS NULL) active config for a given alert type.
     *
     * @param centerId  center identifier
     * @param alertType alert type string
     * @return matching center-level active config, or empty if none found
     */
    Optional<NotificationChannelConfig> findByCenterIdAndWarehouseIdIsNullAndAlertTypeAndActiveTrue(
            Long centerId, String alertType);
}