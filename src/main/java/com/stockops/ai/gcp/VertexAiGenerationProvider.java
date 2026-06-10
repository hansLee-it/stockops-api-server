package com.stockops.ai.gcp;

import com.stockops.ai.provider.AiGenerationProvider;
import com.stockops.ai.provider.AiGenerationRequest;
import com.stockops.ai.provider.AiGenerationResponse;
import com.stockops.ai.provider.AiServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VertexAiGenerationProvider implements AiGenerationProvider {

    private static final Logger log = LoggerFactory.getLogger(VertexAiGenerationProvider.class);

    private final VertexAiProperties properties;

    public VertexAiGenerationProvider(final VertexAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerId() {
        return "vertex-ai";
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public AiGenerationResponse generate(final AiGenerationRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Vertex AI provider is disabled");
        }
        if (properties.getProjectId() == null || properties.getProjectId().isBlank()) {
            throw new IllegalStateException("Vertex AI project id is not configured");
        }

        final String combinedPrompt = request.systemPrompt() + "\n\n" + request.userPrompt();
        final com.google.genai.types.GenerateContentResponse response =
                callApi(properties.getModelId(), combinedPrompt);

        final String text = response.text();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Vertex AI response was empty");
        }

        // Extract token usage from usageMetadata (google-genai 1.5.0)
        // usageMetadata() → Optional<GenerateContentResponseUsageMetadata>
        // promptTokenCount() / candidatesTokenCount() → Optional<Integer>
        Integer inputTokens = null;
        Integer outputTokens = null;
        try {
            final var metaOpt = response.usageMetadata();
            if (metaOpt.isPresent()) {
                final var meta = metaOpt.get();
                inputTokens = meta.promptTokenCount().orElse(null);
                outputTokens = meta.candidatesTokenCount().orElse(null);
                log.debug("[Vertex] Token usage — input: {}, output: {}", inputTokens, outputTokens);
            }
        } catch (final Exception e) {
            log.debug("[Vertex] Could not extract token usage from usageMetadata: {}", e.getMessage());
        }

        final String notice = request.chatVisible() ? properties.getFallbackNotice() : "";
        return new AiGenerationResponse(
                text,
                providerId(),
                properties.getModelId(),
                AiServiceStatus.FALLBACK_ACTIVE,
                true,
                "BEDROCK_PROVIDER_UNAVAILABLE",
                notice,
                "",
                inputTokens,
                outputTokens);
    }

    /**
     * Calls the Vertex AI API and returns the raw response.
     * Protected to allow stubbing in unit tests without hitting the network.
     */
    protected com.google.genai.types.GenerateContentResponse callApi(
            final String modelId, final String prompt) {
        final com.google.genai.Client client = com.google.genai.Client.builder()
                .vertexAI(true)
                .project(properties.getProjectId())
                .location(properties.getLocation())
                .build();
        return client.models.generateContent(modelId, prompt, null);
    }
}
