package com.stockops.ai.provider;

import com.stockops.ai.bedrock.BedrockGenerationProvider;
import com.stockops.ai.gcp.VertexAiGenerationProvider;
import com.stockops.ai.gcp.VertexAiProperties;
import com.stockops.ai.metrics.AiCallMetrics;
import com.stockops.ai.metrics.AiCallRecord;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiProviderFacade {

    private final BedrockGenerationProvider bedrockProvider;
    private final VertexAiGenerationProvider vertexProvider;
    private final VertexAiProperties vertexProperties;
    private final AiCallMetrics aiCallMetrics;

    public AiProviderFacade(final BedrockGenerationProvider bedrockProvider,
                            final VertexAiGenerationProvider vertexProvider,
                            final VertexAiProperties vertexProperties,
                            final AiCallMetrics aiCallMetrics) {
        this.bedrockProvider = bedrockProvider;
        this.vertexProvider = vertexProvider;
        this.vertexProperties = vertexProperties;
        this.aiCallMetrics = aiCallMetrics;
    }

    public AiGenerationResponse generate(final AiGenerationRequest request) {
        final long start = System.currentTimeMillis();
        final String requestId = UUID.randomUUID().toString();

        if (!bedrockProvider.isEnabled() && !vertexProvider.isEnabled()) {
            aiCallMetrics.record(new AiCallRecord(
                    requestId, "none", "", request.useCase(),
                    false, false, "UNCONFIGURED",
                    System.currentTimeMillis() - start, null, null, Instant.now()));
            return noConfiguredProvider(request);
        }

        try {
            if (bedrockProvider.isEnabled()) {
                final AiGenerationResponse response = bedrockProvider.generate(request);
                aiCallMetrics.record(new AiCallRecord(
                        requestId, response.provider(), response.modelId(), request.useCase(),
                        true, false, null,
                        System.currentTimeMillis() - start, response.inputTokens(), response.outputTokens(), Instant.now()));
                return response;
            }
            throw new IllegalStateException("Bedrock provider is disabled");
        } catch (final RuntimeException bedrockFailure) {
            if (isAuthenticationFailure(bedrockFailure) && !vertexProvider.isEnabled()) {
                aiCallMetrics.record(new AiCallRecord(
                        requestId, "bedrock", "", request.useCase(),
                        false, false, "UNAUTHENTICATED",
                        System.currentTimeMillis() - start, null, null, Instant.now()));
                return unauthenticated(request);
            }
            if (!vertexProvider.isEnabled()) {
                aiCallMetrics.record(new AiCallRecord(
                        requestId, "bedrock", "", request.useCase(),
                        false, false, truncate(bedrockFailure.getMessage()),
                        System.currentTimeMillis() - start, null, null, Instant.now()));
                throw bedrockFailure;
            }
            try {
                final AiGenerationResponse response = vertexProvider.generate(request);
                aiCallMetrics.record(new AiCallRecord(
                        requestId, response.provider(), response.modelId(), request.useCase(),
                        true, true, null,
                        System.currentTimeMillis() - start, response.inputTokens(), response.outputTokens(), Instant.now()));
                return response;
            } catch (final RuntimeException vertexFailure) {
                if (isAuthenticationFailure(vertexFailure)) {
                    aiCallMetrics.record(new AiCallRecord(
                            requestId, "vertex", "", request.useCase(),
                            false, true, "UNAUTHENTICATED",
                            System.currentTimeMillis() - start, null, null, Instant.now()));
                    return unauthenticated(request);
                }
                aiCallMetrics.record(new AiCallRecord(
                        requestId, "vertex", "", request.useCase(),
                        false, true, truncate(vertexFailure.getMessage()),
                        System.currentTimeMillis() - start, null, null, Instant.now()));
                throw vertexFailure;
            }
        }
    }

    private static String truncate(final String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private AiGenerationResponse noConfiguredProvider(final AiGenerationRequest request) {
        return new AiGenerationResponse(
                "",
                "none",
                "",
                AiServiceStatus.UNCONFIGURED,
                false,
                "AI_SERVICE_UNCONFIGURED",
                "",
                request.chatVisible() ? vertexProperties.getNoServiceNotice() : "",
                null,
                null);
    }

    private AiGenerationResponse unauthenticated(final AiGenerationRequest request) {
        return new AiGenerationResponse(
                "",
                "none",
                "",
                AiServiceStatus.UNAUTHENTICATED,
                false,
                "AI_SERVICE_UNAUTHENTICATED",
                "",
                request.chatVisible() ? vertexProperties.getUnauthenticatedNotice() : "",
                null,
                null);
    }

    private boolean isAuthenticationFailure(final RuntimeException failure) {
        final String message = failure.getMessage() == null ? "" : failure.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("accessdenied")
                || message.contains("unauthorized")
                || message.contains("unauthenticated")
                || message.contains("permission denied")
                || message.contains("credentials");
    }
}
