package com.stockops.ai.bedrock;

import com.stockops.ai.bedrock.agent.AgentToolDispatcher;
import com.stockops.ai.bedrock.agent.AgentToolResult;
import com.stockops.ai.bedrock.dto.BedrockAgentInvokeRequest;
import com.stockops.ai.bedrock.dto.BedrockAgentInvokeResponse;
import com.stockops.ai.bedrock.dto.BedrockRagQueryRequest;
import com.stockops.ai.bedrock.dto.BedrockRagQueryResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateType;

@Component
public class BedrockAgentRuntimeClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(BedrockAgentRuntimeClientAdapter.class);

    private final BedrockAiProperties properties;
    private final AgentToolDispatcher toolDispatcher;

    public BedrockAgentRuntimeClientAdapter(final BedrockAiProperties properties,
                                            final AgentToolDispatcher toolDispatcher) {
        this.properties = properties;
        this.toolDispatcher = toolDispatcher;
    }

    public BedrockRagQueryResponse retrieveAndGenerate(final BedrockRagQueryRequest request) {
        if (!properties.isEnabled()
                || properties.getKnowledgeBaseId() == null
                || properties.getKnowledgeBaseId().isBlank()) {
            return new BedrockRagQueryResponse(
                    "Knowledge Base is not configured.",
                    List.of(),
                    null);
        }

        final String modelArn = properties.generationModelReference();
        try (BedrockAgentRuntimeClient client = BedrockAgentRuntimeClient.builder()
                .region(Region.of(properties.getRegion()))
                .build()) {
            final RetrieveAndGenerateRequest ragRequest = RetrieveAndGenerateRequest.builder()
                    .input(RetrieveAndGenerateInput.builder().text(request.question()).build())
                    .retrieveAndGenerateConfiguration(RetrieveAndGenerateConfiguration.builder()
                            .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)
                            .knowledgeBaseConfiguration(KnowledgeBaseRetrieveAndGenerateConfiguration.builder()
                                    .knowledgeBaseId(properties.getKnowledgeBaseId())
                                    .modelArn(modelArn)
                                    .build())
                            .build())
                    .build();
            final RetrieveAndGenerateResponse response = client.retrieveAndGenerate(ragRequest);
            final List<String> citations = extractCitations(response);
            return new BedrockRagQueryResponse(
                    response.output().text(),
                    citations,
                    response.sessionId());
        } catch (final Exception e) {
            log.error("Bedrock RAG query failed: {}", e.getMessage(), e);
            return new BedrockRagQueryResponse(
                    "Knowledge Base 조회 중 오류가 발생했습니다.",
                    List.of(),
                    null);
        }
    }

    /**
     * Invokes the Bedrock Agent with return-control support.
     *
     * <p>When the Bedrock Agent issues a {@code returnControl} event (requesting a tool call),
     * the request is dispatched to {@link AgentToolDispatcher}, and the result is logged.
     * Full streaming invocation via {@code InvokeAgentRequest} requires live AWS credentials
     * and is gated on {@code stockops.ai.bedrock.enabled} + agent configuration.
     *
     * <p>TODO (live credentials required): Replace the stub below with the actual
     * {@code BedrockAgentRuntimeClient.invokeAgent()} streaming call, handling
     * {@code returnControlPayload} events by calling
     * {@code toolDispatcher.dispatch(toolName, inputJson)} and submitting the
     * result back to the agent via {@code InvokeAgentRequest.sessionState}.
     */
    public BedrockAgentInvokeResponse invokeAgent(final BedrockAgentInvokeRequest request) {
        if (!properties.isEnabled()
                || properties.getAgentId() == null || properties.getAgentId().isBlank()
                || properties.getAgentAliasId() == null || properties.getAgentAliasId().isBlank()) {
            return new BedrockAgentInvokeResponse(
                    "Bedrock Agent is not configured.",
                    request.sessionId(),
                    false);
        }

        log.info("Bedrock Agent invocation: agentId={}, sessionId={}", properties.getAgentId(), request.sessionId());

        // Dispatcher is wired and ready for live return-control event handling.
        // Preview: exercise the dispatcher with the request text as a synthetic tool call.
        final AgentToolResult preview = toolDispatcher.dispatch(
                "getInventoryRisk", "{\"productId\": null}");
        log.debug("[Agent] Tool dispatcher preview result: success={}", preview.success());

        return new BedrockAgentInvokeResponse(
                "Bedrock Agent 준비 완료 — live AWS 자격 증명 설정 후 실제 호출이 활성화됩니다.",
                request.sessionId(),
                false);
    }

    private List<String> extractCitations(final RetrieveAndGenerateResponse response) {
        final List<String> citations = new ArrayList<>();
        if (response.citations() != null) {
            for (final var citation : response.citations()) {
                if (citation.retrievedReferences() != null) {
                    for (final var ref : citation.retrievedReferences()) {
                        if (ref.location() != null && ref.location().s3Location() != null) {
                            citations.add(ref.location().s3Location().uri());
                        }
                    }
                }
            }
        }
        return citations;
    }
}
