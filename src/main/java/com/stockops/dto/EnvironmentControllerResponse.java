package com.stockops.dto;

import com.stockops.entity.ControllerStatus;
import com.stockops.entity.ControllerType;
import com.stockops.entity.EnvironmentAxis;
import java.time.Instant;

/**
 * Environment controller response payload.
 *
 * @param id controller identifier
 * @param name controller display name
 * @param siteId Sensimul site identifier
 * @param controllerId Sensimul controller identifier
 * @param controllerType controller type
 * @param targetAxis target environment axis
 * @param status controller status
 * @param outputLevel controller output level
 * @param mqttTopic canonical controller topic
 * @param active whether the controller is active
 * @param deleted whether the controller is soft-deleted
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record EnvironmentControllerResponse(
        Long id,
        String name,
        String siteId,
        String controllerId,
        ControllerType controllerType,
        EnvironmentAxis targetAxis,
        ControllerStatus status,
        Integer outputLevel,
        String mqttTopic,
        boolean active,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt) {
}
