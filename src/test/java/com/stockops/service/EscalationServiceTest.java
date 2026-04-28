package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stockops.notification.escalation.EscalationPolicy;
import com.stockops.notification.escalation.EscalationPolicyRepository;
import com.stockops.notification.escalation.EscalationRule;
import com.stockops.notification.escalation.EscalationService;
import com.stockops.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock
    private EscalationPolicyRepository policyRepository;

    @InjectMocks
    private EscalationService escalationService;

    @Test
    void resolvePolicyPrefersWarehouseSpecific() {
        final EscalationPolicy warehousePolicy = new EscalationPolicy();
        warehousePolicy.setId(1L);
        warehousePolicy.setCenterId(10L);
        warehousePolicy.setWarehouseId(20L);
        warehousePolicy.setAlertType("TEMPERATURE");

        when(policyRepository.findByCenterIdAndWarehouseIdAndAlertTypeAndActiveTrue(10L, 20L, "TEMPERATURE"))
                .thenReturn(Optional.of(warehousePolicy));

        final Optional<EscalationPolicy> result = escalationService.resolvePolicy(10L, 20L, "TEMPERATURE");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void resolvePolicyFallsBackToCenterLevel() {
        final EscalationPolicy centerPolicy = new EscalationPolicy();
        centerPolicy.setId(2L);
        centerPolicy.setCenterId(10L);
        centerPolicy.setWarehouseId(null);
        centerPolicy.setAlertType("TEMPERATURE");

        when(policyRepository.findByCenterIdAndWarehouseIdAndAlertTypeAndActiveTrue(10L, 20L, "TEMPERATURE"))
                .thenReturn(Optional.empty());
        when(policyRepository.findByCenterIdAndWarehouseIdIsNullAndAlertTypeAndActiveTrue(10L, "TEMPERATURE"))
                .thenReturn(Optional.of(centerPolicy));

        final Optional<EscalationPolicy> result = escalationService.resolvePolicy(10L, 20L, "TEMPERATURE");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(2L);
    }

    @Test
    void getEscalationRulesThrowsWhenPolicyNotFound() {
        when(policyRepository.findByIdWithRules(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> escalationService.getEscalationRules(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("EscalationPolicy not found: 99");
    }
}
