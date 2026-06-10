package com.stockops.ai.bedrock.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response for the operations summary AI call.
 *
 * <p>Per §5.4 output spec: urgent items, risk summary, suggested actions,
 * source counts, confidence caveat.
 *
 * <p>{@code sourceCounts} is computed deterministically from the data sources queried —
 * it is never derived from the AI response text (§8 policy).
 * {@code confidenceCaveat} is a deterministic disclaimer based on data volume.
 */
public record BedrockOpsSummaryResponse(
        LocalDate businessDate,
        Long centerId,
        Long warehouseId,
        String summary,
        List<String> urgentItems,
        List<String> recommendedActions,
        String riskLevel,
        Instant generatedAt,
        Map<String, Integer> sourceCounts,
        String confidenceCaveat) {
}
