package com.stockops.ai.bedrock.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.dto.AIRecommendationDTO;
import com.stockops.dto.InventoryDTO;
import com.stockops.service.EnvironmentQueryService;
import com.stockops.service.InventoryQueryService;
import com.stockops.service.ai.AIRecommendationService;
import com.stockops.service.ai.AISuggestionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentToolDispatcherTest {

    @Mock
    private InventoryQueryService inventoryQueryService;
    @Mock
    private AIRecommendationService recommendationService;
    @Mock
    private EnvironmentQueryService environmentQueryService;
    @Mock
    private AISuggestionService aiSuggestionService;

    private AgentToolDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new AgentToolDispatcher(
                inventoryQueryService,
                recommendationService,
                environmentQueryService,
                aiSuggestionService);
    }

    @Test
    void dispatch_getInventoryRisk_withoutProductId_callsGetAllInventory() {
        when(inventoryQueryService.getAllInventory()).thenReturn(List.of());

        final AgentToolResult result = dispatcher.dispatch("getInventoryRisk", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.toolName()).isEqualTo("getInventoryRisk");
        assertThat(result.resultJson()).isNotNull();
        verify(inventoryQueryService).getAllInventory();
    }

    @Test
    void dispatch_getInventoryRisk_withProductId_callsGetInventoryByProduct() {
        when(inventoryQueryService.getInventoryByProduct(42L)).thenReturn(List.of());

        final AgentToolResult result = dispatcher.dispatch("getInventoryRisk", "{\"productId\": 42}");

        assertThat(result.success()).isTrue();
        verify(inventoryQueryService).getInventoryByProduct(42L);
    }

    @Test
    void dispatch_getForecastRecommendation_callsListRecommendations() {
        when(recommendationService.listRecommendations(any(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        final AgentToolResult result = dispatcher.dispatch("getForecastRecommendation", "{}");

        assertThat(result.success()).isTrue();
        verify(recommendationService).listRecommendations(any(), isNull(), isNull(), isNull());
    }

    @Test
    void dispatch_getSensorAnomalies_callsGetAlerts() {
        when(environmentQueryService.getAlerts(7)).thenReturn(List.of());

        final AgentToolResult result = dispatcher.dispatch("getSensorAnomalies", "{\"days\": 7}");

        assertThat(result.success()).isTrue();
        verify(environmentQueryService).getAlerts(7);
    }

    @Test
    void dispatch_unknownTool_returnsFailure() {
        final AgentToolResult result = dispatcher.dispatch("nonExistentTool", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Unknown tool");
    }

    @Test
    void dispatch_nullInput_doesNotThrow() {
        when(inventoryQueryService.getAllInventory()).thenReturn(List.of());

        final AgentToolResult result = dispatcher.dispatch("getInventoryRisk", null);

        assertThat(result.success()).isTrue();
    }

    @Test
    void dispatch_malformedJson_returnsFailure() {
        final AgentToolResult result = dispatcher.dispatch("getInventoryRisk", "not-json");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }
}
