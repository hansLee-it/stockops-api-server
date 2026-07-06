package com.stockops.notification.escalation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * DTO for EscalationPolicy responses.
 *
 * @author StockOps Team
 * @since 2.0
 */
public record EscalationPolicyDTO(
        Long id,
        Long centerId,
        Long warehouseId,
        String alertType,
        boolean active,
        List<EscalationRuleDTO> rules,
        Instant createdAt,
        Instant updatedAt
) {}

/**
 * DTO for EscalationRule responses.
 *
 * @author StockOps Team
 * @since 2.0
 */
record EscalationRuleDTO(
        Long id,
        Integer level,
        Integer delayMinutes,
        List<String> notifyRoles,
        List<String> channels
) {}

/**
 * DTO for creating/updating an EscalationPolicy.
 *
 * @author StockOps Team
 * @since 2.0
 */
record EscalationPolicyRequest(
        @NotNull Long centerId,
        Long warehouseId,
        @NotBlank String alertType,
        Boolean active,
        List<EscalationRuleRequest> rules
) {}

/**
 * DTO for creating/updating an EscalationRule within a policy.
 *
 * @author StockOps Team
 * @since 2.0
 */
record EscalationRuleRequest(
        Integer level,
        Integer delayMinutes,
        List<String> notifyRoles,
        List<String> channels
) {}