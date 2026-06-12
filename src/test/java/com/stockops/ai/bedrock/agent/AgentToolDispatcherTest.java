package com.stockops.ai.bedrock.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.ai.forecast.AiForecastClient;
import com.stockops.dto.AIRecommendationDTO;
import com.stockops.dto.InventoryDTO;
import com.stockops.entity.PurchaseOrderShipment;
import com.stockops.repository.PurchaseOrderShipmentRepository;
import com.stockops.service.EnvironmentQueryService;
import com.stockops.service.InventoryQueryService;
import com.stockops.service.ai.AIRecommendationService;
import com.stockops.service.ai.AISuggestionService;
import java.time.LocalDate;
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
    @Mock
    private PurchaseOrderShipmentRepository shipmentRepository;
    @Mock
    private AiForecastClient aiForecastClient;

    private AgentToolDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new AgentToolDispatcher(
                inventoryQueryService,
                recommendationService,
                environmentQueryService,
                aiSuggestionService,
                shipmentRepository,
                aiForecastClient);
    }

    @Test
    void dispatch_getProphetForecast_mapsForecastPoints() {
        when(aiForecastClient.getForecast(1L, 7)).thenReturn(new AiForecastClient.AiForecastResponse(
                1L, 7, List.of(new AiForecastClient.AiForecastResponse.ForecastPoint(
                        "2026-06-12", 12.4, 8.1, 16.8))));

        final AgentToolResult result = dispatcher.dispatch("getProphetForecast", "{\"productId\": 1, \"days\": 7}");

        assertThat(result.success()).isTrue();
        assertThat(result.toolName()).isEqualTo("getProphetForecast");
        assertThat(result.resultJson()).contains("\"provider\":\"prophet\"");
        assertThat(result.resultJson()).contains("\"date\":\"2026-06-12\"");
        assertThat(result.resultJson()).contains("\"predictedQuantity\":12.4");
        assertThat(result.resultJson()).contains("\"lower\":8.1");
        assertThat(result.resultJson()).contains("\"upper\":16.8");
        assertThat(result.resultJson()).contains("\"fallbackUsed\":false");
    }

    @Test
    void dispatch_getProphetForecast_defaultsAndCapsDays() {
        when(aiForecastClient.getForecast(1L, 7)).thenReturn(new AiForecastClient.AiForecastResponse(1L, 7, List.of()));

        dispatcher.dispatch("getProphetForecast", "{\"productId\": 1}");
        verify(aiForecastClient).getForecast(1L, 7);

        when(aiForecastClient.getForecast(1L, 30)).thenReturn(new AiForecastClient.AiForecastResponse(1L, 30, List.of()));
        dispatcher.dispatch("getProphetForecast", "{\"productId\": 1, \"days\": 90}");
        verify(aiForecastClient).getForecast(1L, 30);
    }

    @Test
    void dispatch_getProphetForecast_withoutProductId_returnsFailure() {
        final AgentToolResult result = dispatcher.dispatch("getProphetForecast", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("productId");
    }

    @Test
    void dispatch_getProphetForecast_serviceUnavailable_returnsFailure() {
        when(aiForecastClient.getForecast(1L, 7)).thenReturn(null);

        final AgentToolResult result = dispatcher.dispatch("getProphetForecast", "{\"productId\": 1}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Prophet");
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
    void dispatch_getPurchaseOrderDelaySummary_emptyList_returnsEmptyJson() {
        when(shipmentRepository.findByEtaDateBeforeAndDeliveredAtIsNull(
                org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(List.of());

        final AgentToolResult result = dispatcher.dispatch("getPurchaseOrderDelaySummary", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.resultJson()).isEqualTo("[]");
    }

    @Test
    void dispatch_getPurchaseOrderDelaySummary_withOverdueShipment_includesDaysOverdue() {
        final PurchaseOrderShipment shipment = new PurchaseOrderShipment();
        shipment.setShipmentNumber("SHP-001");
        shipment.setCarrier("FedEx");
        shipment.setEtaDate(LocalDate.now().minusDays(3));

        when(shipmentRepository.findByEtaDateBeforeAndDeliveredAtIsNull(
                org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(List.of(shipment));

        final AgentToolResult result = dispatcher.dispatch("getPurchaseOrderDelaySummary", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.resultJson()).contains("\"daysOverdue\":3");
        assertThat(result.resultJson()).contains("SHP-001");
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
