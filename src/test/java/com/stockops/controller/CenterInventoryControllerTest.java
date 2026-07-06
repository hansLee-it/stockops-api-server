package com.stockops.controller;

import com.stockops.exception.ForbiddenException;
import com.stockops.security.ScopeGuard;
import com.stockops.service.CenterInventoryAggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies center inventory access is guarded by the caller's scope (A-4 IDOR fix).
 */
@ExtendWith(MockitoExtension.class)
class CenterInventoryControllerTest {

    @Mock
    private CenterInventoryAggregationService centerInventoryService;

    @Mock
    private ScopeGuard scopeGuard;

    @InjectMocks
    private CenterInventoryController controller;

    @Test
    void deniesInventoryForOutOfScopeCenter() {
        doThrow(new ForbiddenException("Access denied for center: 99"))
                .when(scopeGuard).assertCenterAccess(99L);

        assertThatThrownBy(() -> controller.getCenterInventory(99L))
                .isInstanceOf(ForbiddenException.class);

        verify(centerInventoryService, never()).getCenterInventorySummary(99L);
    }

    @Test
    void returnsInventoryForInScopeCenter() {
        when(centerInventoryService.getCenterInventorySummary(1L))
                .thenReturn(Map.of("centerId", 1L));

        Map<String, Object> result = controller.getCenterInventory(1L);

        verify(scopeGuard).assertCenterAccess(1L);
        assertThat(result).containsEntry("centerId", 1L);
    }
}
