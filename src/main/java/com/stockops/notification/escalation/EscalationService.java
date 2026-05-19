package com.stockops.notification.escalation;

import com.stockops.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing escalation policies and resolving rules for alerts.
 * Resolution logic: warehouse-specific policy takes precedence over center-level policy.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
@Transactional
public class EscalationService {

    private final EscalationPolicyRepository policyRepository;

    /**
     * Resolves the most specific escalation policy for a given alert context.
     * Prefers warehouse-specific policy over center-level fallback.
     *
     * @param centerId center identifier
     * @param warehouseId warehouse identifier (nullable for center-level lookup)
     * @param alertType alert type string (e.g. TEMPERATURE, HUMIDITY)
     * @return matching active policy, or empty if none found
     */
    @Transactional(readOnly = true)
    public Optional<EscalationPolicy> resolvePolicy(Long centerId, Long warehouseId, String alertType) {
        if (warehouseId != null) {
            Optional<EscalationPolicy> warehousePolicy =
                    policyRepository.findByCenterIdAndWarehouseIdAndAlertTypeAndActiveTrue(
                            centerId, warehouseId, alertType);
            if (warehousePolicy.isPresent()) {
                return warehousePolicy;
            }
        }
        return policyRepository.findByCenterIdAndWarehouseIdIsNullAndAlertTypeAndActiveTrue(centerId, alertType);
    }

    /**
     * Returns escalation rules for a policy, ordered by level ascending.
     *
     * @param policyId policy identifier
     * @return ordered list of escalation rules
     * @throws ResourceNotFoundException if policy not found
     */
    @Transactional(readOnly = true)
    public List<EscalationRule> getEscalationRules(Long policyId) {
        EscalationPolicy policy = policyRepository.findByIdWithRules(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("EscalationPolicy not found: " + policyId));
        return policy.getRules().stream()
                .sorted(Comparator.comparingInt(EscalationRule::getLevel))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EscalationPolicy> findAllByCenterId(Long centerId) {
        return policyRepository.findAllWithRulesByCenterId(centerId);
    }

    @Transactional(readOnly = true)
    public EscalationPolicy findById(Long id) {
        return policyRepository.findByIdWithRules(id)
                .orElseThrow(() -> new ResourceNotFoundException("EscalationPolicy not found: " + id));
    }

    /**
     * Creates a new escalation policy with its rules.
     *
     * @param request policy creation request
     * @return created policy with rules
     */
    public EscalationPolicy create(EscalationPolicyRequest request) {
        EscalationPolicy policy = new EscalationPolicy();
        policy.setCenterId(request.centerId());
        policy.setWarehouseId(request.warehouseId());
        policy.setAlertType(request.alertType());
        policy.setActive(request.active() != null ? request.active() : true);

        if (request.rules() != null) {
            for (EscalationRuleRequest ruleReq : request.rules()) {
                EscalationRule rule = new EscalationRule();
                rule.setLevel(ruleReq.level());
                rule.setDelayMinutes(ruleReq.delayMinutes() != null ? ruleReq.delayMinutes() : 0);
                rule.setNotifyRoles(ruleReq.notifyRoles() != null ? ruleReq.notifyRoles() : List.of());
                rule.setChannels(ruleReq.channels() != null ? ruleReq.channels() : List.of("EMAIL"));
                rule.setPolicy(policy);
                policy.getRules().add(rule);
            }
        }

        return policyRepository.save(policy);
    }

    /**
     * Updates an existing escalation policy and replaces its rules.
     *
     * @param id policy identifier
     * @param request update request
     * @return updated policy
     * @throws ResourceNotFoundException if policy not found
     */
    public EscalationPolicy update(Long id, EscalationPolicyRequest request) {
        EscalationPolicy policy = findById(id);

        policy.setCenterId(request.centerId());
        policy.setWarehouseId(request.warehouseId());
        policy.setAlertType(request.alertType());
        if (request.active() != null) {
            policy.setActive(request.active());
        }

        policy.getRules().clear();
        if (request.rules() != null) {
            for (EscalationRuleRequest ruleReq : request.rules()) {
                EscalationRule rule = new EscalationRule();
                rule.setLevel(ruleReq.level());
                rule.setDelayMinutes(ruleReq.delayMinutes() != null ? ruleReq.delayMinutes() : 0);
                rule.setNotifyRoles(ruleReq.notifyRoles() != null ? ruleReq.notifyRoles() : List.of());
                rule.setChannels(ruleReq.channels() != null ? ruleReq.channels() : List.of("EMAIL"));
                rule.setPolicy(policy);
                policy.getRules().add(rule);
            }
        }

        return policyRepository.save(policy);
    }

    /**
     * Soft-deletes a policy by setting active = false.
     *
     * @param id policy identifier
     * @throws ResourceNotFoundException if policy not found
     */
    public void delete(Long id) {
        EscalationPolicy policy = findById(id);
        policy.setActive(false);
        policyRepository.save(policy);
    }

    public EscalationService(final EscalationPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

}