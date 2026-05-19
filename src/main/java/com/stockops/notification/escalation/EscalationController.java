package com.stockops.notification.escalation;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for escalation policy CRUD and resolution.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/escalation-policies")
public class EscalationController {

    private final EscalationService escalationService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public List<EscalationPolicyDTO> listPolicies(
            @RequestParam Long centerId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String alertType) {

        List<EscalationPolicy> policies = escalationService.findAllByCenterId(centerId);

        return policies.stream()
                .filter(p -> warehouseId == null || warehouseId.equals(p.getWarehouseId()) || p.getWarehouseId() == null)
                .filter(p -> alertType == null || alertType.equals(p.getAlertType()))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public EscalationPolicyDTO getPolicy(@PathVariable Long id) {
        return toDto(escalationService.findById(id));
    }

    @GetMapping("/resolve")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public ResponseEntity<EscalationPolicyDTO> resolvePolicy(
            @RequestParam Long centerId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam String alertType) {

        return escalationService.resolvePolicy(centerId, warehouseId, alertType)
                .map(policy -> ResponseEntity.ok(toDto(policy)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_CREATE')")
    public EscalationPolicyDTO createPolicy(@RequestBody EscalationPolicyRequest request) {
        return toDto(escalationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_UPDATE')")
    public EscalationPolicyDTO updatePolicy(@PathVariable Long id,
                                             @RequestBody EscalationPolicyRequest request) {
        return toDto(escalationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_DELETE')")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        escalationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EscalationPolicyDTO toDto(EscalationPolicy policy) {
        List<EscalationRuleDTO> ruleDtos = policy.getRules().stream()
                .sorted((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
                .map(r -> new EscalationRuleDTO(
                        r.getId(),
                        r.getLevel(),
                        r.getDelayMinutes(),
                        r.getNotifyRoles(),
                        r.getChannels()))
                .toList();

        return new EscalationPolicyDTO(
                policy.getId(),
                policy.getCenterId(),
                policy.getWarehouseId(),
                policy.getAlertType(),
                policy.isActive(),
                ruleDtos,
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }

    public EscalationController(final EscalationService escalationService) {
        this.escalationService = escalationService;
    }

}