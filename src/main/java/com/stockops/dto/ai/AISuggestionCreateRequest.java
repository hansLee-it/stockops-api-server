package com.stockops.dto.ai;

import com.stockops.service.ai.AISuggestionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AISuggestionCreateRequest(
        @NotBlank String type,
        @NotBlank String severity,
        @NotBlank String title,
        @NotBlank String summary,
        @NotBlank String reason,
        @NotBlank String recommendedAction,
        String targetType,
        Long targetId,
        @NotBlank String targetScopeType,
        @NotNull Long targetScopeId,
        String payloadJson,
        Double confidenceScore,
        @NotBlank String source,
        @NotBlank String sourceType,
        String createdFromApp,
        String forecastSourceType,
        Long forecastSourceId,
        String forecastModelVersion,
        Instant forecastGeneratedAt,
        String forecastSourcePayloadJson,
        @NotBlank String visibleToApp,
        @NotBlank String approvalMode,
        Long requestedOnBehalfUserId,
        String requestedScopeType,
        Long requestedScopeId,
        Instant expiresAt
) {

    private static final String EMPTY_JSON_OBJECT = "{}";

    public AISuggestionService.CreateCommand toCommand() {
        return new AISuggestionService.CreateCommand(
                type,
                severity,
                title,
                summary,
                reason,
                recommendedAction,
                targetType,
                targetId,
                targetScopeType,
                targetScopeId,
                defaultBlankJson(payloadJson),
                confidenceScore,
                source,
                sourceType,
                createdFromApp,
                forecastSourceType,
                forecastSourceId,
                forecastModelVersion,
                forecastGeneratedAt,
                defaultBlankJson(forecastSourcePayloadJson),
                visibleToApp,
                approvalMode,
                requestedOnBehalfUserId,
                requestedScopeType,
                requestedScopeId,
                expiresAt
        );
    }

    private static String defaultBlankJson(final String value) {
        return value == null || value.isBlank() ? EMPTY_JSON_OBJECT : value;
    }
}
