package com.stockops.ai.bedrock.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockops.dto.AIRecommendationDTO;
import com.stockops.entity.PurchaseOrderShipment;
import com.stockops.entity.ai.AISuggestion;
import com.stockops.repository.PurchaseOrderShipmentRepository;
import com.stockops.service.EnvironmentQueryService;
import com.stockops.service.InventoryQueryService;
import com.stockops.service.ai.AIRecommendationService;
import com.stockops.service.ai.AISuggestionService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispatches Bedrock Agent tool (return-control) calls to the appropriate StockOps service.
 *
 * <p>Supported tools:
 * <ul>
 *   <li>{@code getInventoryRisk} — returns inventory snapshot for a product or all products</li>
 *   <li>{@code getForecastRecommendation} — returns AI recommendations for a given date/scope</li>
 *   <li>{@code getSensorAnomalies} — returns recent sensor alerts</li>
 *   <li>{@code getPurchaseOrderDelaySummary} — returns overdue shipments with days-overdue field</li>
 *   <li>{@code createAISuggestionDraft} — creates a PENDING AISuggestion for human review</li>
 * </ul>
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
public class AgentToolDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AgentToolDispatcher.class);

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final InventoryQueryService inventoryQueryService;
    private final AIRecommendationService recommendationService;
    private final EnvironmentQueryService environmentQueryService;
    private final AISuggestionService aiSuggestionService;
    private final PurchaseOrderShipmentRepository shipmentRepository;

    public AgentToolDispatcher(final InventoryQueryService inventoryQueryService,
                               final AIRecommendationService recommendationService,
                               final EnvironmentQueryService environmentQueryService,
                               final AISuggestionService aiSuggestionService,
                               final PurchaseOrderShipmentRepository shipmentRepository) {
        this.inventoryQueryService = inventoryQueryService;
        this.recommendationService = recommendationService;
        this.environmentQueryService = environmentQueryService;
        this.aiSuggestionService = aiSuggestionService;
        this.shipmentRepository = shipmentRepository;
    }

    /**
     * Dispatches an agent tool call.
     * The method is read-only transactional so lazy-loaded relationships (e.g. shipment → PO)
     * can be accessed safely within the scope of a single transaction.
     *
     * @param toolName  the agent-specified tool name
     * @param inputJson JSON string containing tool arguments from the agent
     * @return result of the tool invocation
     */
    @Transactional(readOnly = true)
    public AgentToolResult dispatch(final String toolName, final String inputJson) {
        log.debug("[Agent] Dispatching tool: {} input={}", toolName, inputJson);
        try {
            final JsonNode input = JSON.readTree(inputJson != null ? inputJson : "{}");
            return switch (toolName) {
                case "getInventoryRisk" -> handleInventoryRisk(input);
                case "getForecastRecommendation" -> handleForecastRecommendation(input);
                case "getSensorAnomalies" -> handleSensorAnomalies(input);
                case "getPurchaseOrderDelaySummary" -> handlePurchaseOrderDelaySummary(input);
                case "createAISuggestionDraft" -> handleCreateAISuggestionDraft(input);
                default -> {
                    log.warn("[Agent] Unknown tool: {}", toolName);
                    yield AgentToolResult.failure(toolName, "Unknown tool: " + toolName);
                }
            };
        } catch (final Exception e) {
            log.error("[Agent] Tool dispatch failed for {}: {}", toolName, e.getMessage(), e);
            return AgentToolResult.failure(toolName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Tool handlers
    // -------------------------------------------------------------------------

    private AgentToolResult handleInventoryRisk(final JsonNode input) throws JsonProcessingException {
        final Long productId = longOrNull(input, "productId");
        final Object result;
        if (productId != null) {
            result = inventoryQueryService.getInventoryByProduct(productId);
        } else {
            result = inventoryQueryService.getAllInventory();
        }
        return AgentToolResult.success("getInventoryRisk", JSON.writeValueAsString(result));
    }

    private AgentToolResult handleForecastRecommendation(final JsonNode input) throws JsonProcessingException {
        final LocalDate businessDate = input.has("businessDate")
                ? LocalDate.parse(input.get("businessDate").asText())
                : LocalDate.now();
        final Long centerId = longOrNull(input, "centerId");
        final Long warehouseId = longOrNull(input, "warehouseId");
        final List<AIRecommendationDTO> recommendations =
                recommendationService.listRecommendations(businessDate, centerId, warehouseId, null);
        return AgentToolResult.success("getForecastRecommendation", JSON.writeValueAsString(recommendations));
    }

    private AgentToolResult handleSensorAnomalies(final JsonNode input) throws JsonProcessingException {
        final int days = input.has("days") ? input.get("days").asInt(7) : 7;
        final Object alerts = environmentQueryService.getAlerts(days);
        return AgentToolResult.success("getSensorAnomalies", JSON.writeValueAsString(alerts));
    }

    private AgentToolResult handlePurchaseOrderDelaySummary(final JsonNode input) throws JsonProcessingException {
        final LocalDate today = LocalDate.now();
        final List<PurchaseOrderShipment> overdueShipments =
                shipmentRepository.findByEtaDateBeforeAndDeliveredAtIsNull(today);

        final List<Map<String, Object>> summary = overdueShipments.stream()
                .map(s -> {
                    final Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("shipmentId", s.getId());
                    entry.put("shipmentNumber", s.getShipmentNumber());
                    entry.put("poNumber", s.getPurchaseOrder() != null ? s.getPurchaseOrder().getPoNumber() : null);
                    entry.put("carrier", s.getCarrier());
                    entry.put("etaDate", s.getEtaDate() != null ? s.getEtaDate().toString() : null);
                    entry.put("daysOverdue", s.getEtaDate() != null
                            ? (int) (today.toEpochDay() - s.getEtaDate().toEpochDay()) : null);
                    return entry;
                })
                .toList();

        return AgentToolResult.success("getPurchaseOrderDelaySummary", JSON.writeValueAsString(summary));
    }

    private AgentToolResult handleCreateAISuggestionDraft(final JsonNode input) throws JsonProcessingException {
        final String type = text(input, "type", "AGENT_SUGGESTION");
        final String severity = text(input, "severity", "MEDIUM");
        final String title = text(input, "title", "Bedrock Agent 제안");
        final String summary = text(input, "summary", "");
        final String reason = text(input, "reason", "Bedrock Agent 자동 분석");
        final String recommendedAction = text(input, "recommendedAction", "");
        final String targetScopeType = text(input, "targetScopeType", "GLOBAL");
        final Long targetScopeId = longOrNull(input, "targetScopeId");

        final AISuggestionService.CreateCommand command = new AISuggestionService.CreateCommand(
                type, severity, title, summary, reason, recommendedAction,
                null, null,
                targetScopeType, targetScopeId,
                null, null,
                "BEDROCK_AGENT", "AI_AGENT",
                null, null, null, null, null, null, null,
                "MANUAL_APPROVAL_REQUIRED",
                null, null, null, null);

        final AISuggestion suggestion = aiSuggestionService.create(command, null, UUID.randomUUID().toString());
        final var responsePayload = new LinkedHashMap<String, Object>();
        responsePayload.put("suggestionId", suggestion.getId());
        responsePayload.put("status", suggestion.getStatus());
        responsePayload.put("title", suggestion.getTitle());
        return AgentToolResult.success("createAISuggestionDraft", JSON.writeValueAsString(responsePayload));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Long longOrNull(final JsonNode node, final String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asLong();
        }
        return null;
    }

    private static String text(final JsonNode node, final String field, final String defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return defaultValue;
    }
}
