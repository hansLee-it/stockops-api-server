package com.stockops.dto.ai;

import com.stockops.entity.ai.AISuggestion;
import com.stockops.entity.ai.AISuggestionStatus;
import java.time.Instant;
import java.util.List;

public record AISuggestionResponse(
        Long id,
        String type,
        String severity,
        String title,
        String summary,
        String reason,
        String recommendedAction,
        String targetType,
        Long targetId,
        AISuggestionStatus status,
        List<String> allowedActions,
        ScopeMetadata scopeMetadata,
        AuditSummary auditSummary,
        String payloadJson,
        Double confidenceScore,
        String source,
        String sourceType,
        Long createdByUserId,
        String createdFromApp,
        String forecastSourceType,
        Long forecastSourceId,
        String forecastModelVersion,
        Instant forecastGeneratedAt,
        String forecastSourcePayloadJson,
        String visibleToApp,
        String approvalMode,
        Long requestedOnBehalfUserId,
        String requestedScopeType,
        Long requestedScopeId,
        Instant expiresAt,
        String errorMessage
) {

    public static AISuggestionResponse from(final AISuggestion suggestion) {
        final List<String> allowedActions = switch (suggestion.getStatus()) {
            case PENDING -> List.of("APPROVE", "REJECT");
            case APPROVED -> List.of("EXECUTE");
            default -> List.of();
        };

        return new AISuggestionResponse(
                suggestion.getId(),
                suggestion.getType(),
                suggestion.getSeverity(),
                suggestion.getTitle(),
                suggestion.getSummary(),
                suggestion.getReason(),
                suggestion.getRecommendedAction(),
                suggestion.getTargetType(),
                suggestion.getTargetId(),
                suggestion.getStatus(),
                allowedActions,
                new ScopeMetadata(
                        suggestion.getTargetScopeType(),
                        suggestion.getTargetScopeId(),
                        suggestion.getRequestedScopeType(),
                        suggestion.getRequestedScopeId(),
                        suggestion.getVisibleToApp(),
                        suggestion.getApprovalMode(),
                        suggestion.getSourceType()),
                new AuditSummary(
                        suggestion.getCreatedByUserId(),
                        suggestion.getCreatedAt(),
                        suggestion.getUpdatedAt(),
                        suggestion.getReviewedByUserId(),
                        suggestion.getReviewedAt(),
                        suggestion.getApprovedByUserId(),
                        suggestion.getApprovedAt(),
                        suggestion.getExecutedAt(),
                        suggestion.getVersion(),
                        suggestion.getSource(),
                        suggestion.getCreatedFromApp()),
                suggestion.getPayloadJson(),
                suggestion.getConfidenceScore(),
                suggestion.getSource(),
                suggestion.getSourceType(),
                suggestion.getCreatedByUserId(),
                suggestion.getCreatedFromApp(),
                suggestion.getForecastSourceType(),
                suggestion.getForecastSourceId(),
                suggestion.getForecastModelVersion(),
                suggestion.getForecastGeneratedAt(),
                suggestion.getForecastSourcePayloadJson(),
                suggestion.getVisibleToApp(),
                suggestion.getApprovalMode(),
                suggestion.getRequestedOnBehalfUserId(),
                suggestion.getRequestedScopeType(),
                suggestion.getRequestedScopeId(),
                suggestion.getExpiresAt(),
                errorMessageFor(suggestion)
        );
    }

    private static String errorMessageFor(final AISuggestion suggestion) {
        return switch (suggestion.getStatus()) {
            case REJECTED -> suggestion.getRejectionReason();
            case FAILED -> suggestion.getExecutionResult();
            default -> null;
        };
    }

    public record ScopeMetadata(
            String targetScopeType,
            Long targetScopeId,
            String requestedScopeType,
            Long requestedScopeId,
            String visibleToApp,
            String approvalMode,
            String sourceType
    ) {
    }

    public record AuditSummary(
            Long createdByUserId,
            Instant createdAt,
            Instant updatedAt,
            Long reviewedByUserId,
            Instant reviewedAt,
            Long approvedByUserId,
            Instant approvedAt,
            Instant executedAt,
            Long version,
            String source,
            String createdFromApp
    ) {
    }
}
