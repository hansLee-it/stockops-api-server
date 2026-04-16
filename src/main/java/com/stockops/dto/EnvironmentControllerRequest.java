package com.stockops.dto;

import com.stockops.entity.ControllerStatus;
import com.stockops.entity.ControllerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Environment controller create/update request payload.
 *
 * @param siteId Sensimul site identifier
 * @param controllerId Sensimul controller identifier
 * @param name controller display name
 * @param controllerType controller type
 * @param status controller status
 * @param outputLevel output level from 0 to 100
 * @author StockOps Team
 * @since 1.0
 */
public record EnvironmentControllerRequest(
        @NotBlank String siteId,
        @NotBlank String controllerId,
        @NotBlank String name,
        @NotNull ControllerType controllerType,
        @NotNull ControllerStatus status,
        @NotNull @Min(0) @Max(100) Integer outputLevel) {
}
