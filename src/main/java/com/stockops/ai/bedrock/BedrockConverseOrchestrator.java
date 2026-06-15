package com.stockops.ai.bedrock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import com.stockops.ai.bedrock.agent.AgentToolCatalog;
import com.stockops.ai.bedrock.agent.AgentToolDispatcher;
import com.stockops.ai.bedrock.agent.AgentToolResult;
import com.stockops.ai.bedrock.dto.BedrockAgentInvokeRequest;
import com.stockops.ai.bedrock.dto.BedrockAgentInvokeResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.GuardrailConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

/**
 * Direct Bedrock Converse tool-use orchestrator (the chosen alternative to a managed Bedrock
 * Agent). Spring owns the loop: it sends the system prompt + tool catalog (+ optional guardrail),
 * and whenever the model returns a {@code tool_use} stop reason it dispatches each requested tool
 * through {@link AgentToolDispatcher}, appends the {@code tool_result}, and re-invokes Converse
 * until a final text answer. Bedrock never touches the DB or ai-module directly — only curated
 * tool results flow back.
 *
 * <p>Gated on {@code stockops.ai.bedrock.enabled} + a configured model. With no live config the
 * call returns a safe "not configured" response so the feature is inert until values are injected.
 *
 * @author StockOps Team
 * @since 2.4
 */
@Service
public class BedrockConverseOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BedrockConverseOrchestrator.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Built-in system prompt. Overridable at runtime via {@code stockops.ai.bedrock.system-prompt}
     * so the assistant's tone/format can be tuned without a redeploy. The {@code [응답 형식]} block
     * keeps answers concise and free of filler.
     */
    static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 StockOps 재고·환경 운영 어시스턴트입니다.

            [근거·안전]
            - 한국어로 답합니다.
            - 모든 수치(재고량·예측값·추천 수량 등)는 도구 결과 또는 제공된 운영 문서를 근거로 인용하고, 임의로 만들지 않습니다.
            - 필요한 도구만 최소한으로 호출합니다. 데이터가 없으면 "없음"이라고 분명히 말합니다.
            - 발주 승인·알림 확인·제어기 명령·입출고 실행 등 실제 운영 변경은 직접 수행하지 않고,
              사용자가 웹에서 승인/실행하도록 안내합니다.

            [응답 형식]
            - 결론을 1~2문장으로 먼저 제시하고, 필요한 근거만 덧붙입니다.
            - 항목이 여러 개면 최대 5개 불릿, 각 불릿은 한 줄로 짧게 씁니다.
            - 인사말·사과·"도와드리겠습니다" 같은 군더더기 없이 본론만 답합니다.
            - 수치는 단위와 함께 표기합니다(예: 120개, 3일분).
            - 표는 비교가 명확히 도움이 될 때만 간단히 사용합니다.
            - 모르거나 권한 밖이면 추측하지 말고 한계를 밝힙니다.""";

    private final BedrockRuntimeClientFactory clientFactory;
    private final BedrockAiProperties properties;
    private final AgentToolCatalog toolCatalog;
    private final AgentToolDispatcher toolDispatcher;

    public BedrockConverseOrchestrator(final BedrockRuntimeClientFactory clientFactory,
                                       final BedrockAiProperties properties,
                                       final AgentToolCatalog toolCatalog,
                                       final AgentToolDispatcher toolDispatcher) {
        this.clientFactory = clientFactory;
        this.properties = properties;
        this.toolCatalog = toolCatalog;
        this.toolDispatcher = toolDispatcher;
    }

    /**
     * Runs the tool-use conversation for a single user message.
     *
     * @param request user message (+ optional scope) and session id
     * @param documentContext optional ops-doc context (from KB Retrieve), prepended to the system
     *     prompt; may be null/blank when KB is not configured
     * @return final assistant answer
     */
    public BedrockAgentInvokeResponse converse(final BedrockAgentInvokeRequest request,
                                               final String documentContext) {
        final String modelReference = properties.generationModelReference();
        if (!properties.isEnabled() || modelReference == null || modelReference.isBlank()) {
            return new BedrockAgentInvokeResponse(
                    "AI 어시스턴트가 아직 구성되지 않았습니다.", request.sessionId(), false);
        }

        final List<SystemContentBlock> system = new ArrayList<>();
        system.add(SystemContentBlock.builder().text(resolveSystemPrompt()).build());
        if (documentContext != null && !documentContext.isBlank()) {
            system.add(SystemContentBlock.builder()
                    .text("참고 운영 문서:\n" + documentContext).build());
        }

        final List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.builder().text(request.message()).build())
                .build());

        try (BedrockRuntimeClient client = clientFactory.create(properties.getRegion())) {
            for (int turn = 0; turn <= properties.getMaxToolTurns(); turn++) {
                final ConverseResponse response = client.converse(buildRequest(modelReference, system, messages));
                final Message assistant = response.output().message();
                messages.add(assistant);

                if (response.stopReason() != StopReason.TOOL_USE) {
                    return new BedrockAgentInvokeResponse(extractText(assistant), request.sessionId(), false);
                }
                if (turn == properties.getMaxToolTurns()) {
                    break;
                }
                messages.add(dispatchToolUses(assistant));
            }
            log.warn("Converse exceeded {} tool turns for session {}",
                    properties.getMaxToolTurns(), request.sessionId());
            return new BedrockAgentInvokeResponse(
                    "도구 호출 횟수를 초과하여 응답을 완료하지 못했습니다.", request.sessionId(), false);
        } catch (final Exception e) {
            log.error("Bedrock Converse tool-use failed: {}", e.getMessage(), e);
            return new BedrockAgentInvokeResponse(
                    "AI 응답 생성 중 오류가 발생했습니다.", request.sessionId(), false);
        }
    }

    /**
     * Returns the runtime-overridden system prompt when configured, otherwise the built-in default.
     */
    private String resolveSystemPrompt() {
        final String configured = properties.getSystemPrompt();
        return configured != null && !configured.isBlank() ? configured : DEFAULT_SYSTEM_PROMPT;
    }

    private ConverseRequest buildRequest(final String modelReference, final List<SystemContentBlock> system,
                                         final List<Message> messages) {
        final ConverseRequest.Builder builder = ConverseRequest.builder()
                .modelId(modelReference)
                .system(system)
                .messages(messages)
                .inferenceConfig(InferenceConfiguration.builder()
                        .temperature((float) properties.getTemperature())
                        .maxTokens(properties.getMaxOutputTokens())
                        .build())
                .toolConfig(toolCatalog.toolConfiguration());
        if (properties.hasGuardrail()) {
            builder.guardrailConfig(GuardrailConfiguration.builder()
                    .guardrailIdentifier(properties.getGuardrailId())
                    .guardrailVersion(properties.getGuardrailVersion())
                    .build());
        }
        return builder.build();
    }

    /**
     * Executes every tool_use block in the assistant message and packages the results as a single
     * USER message of tool_result blocks for the next turn.
     */
    private Message dispatchToolUses(final Message assistant) {
        final List<ContentBlock> toolResults = new ArrayList<>();
        for (final ContentBlock block : assistant.content()) {
            if (block.toolUse() == null) {
                continue;
            }
            final ToolUseBlock toolUse = block.toolUse();
            final String inputJson = documentToJson(toolUse.input());
            final AgentToolResult result = toolDispatcher.dispatch(toolUse.name(), inputJson);
            toolResults.add(ContentBlock.fromToolResult(ToolResultBlock.builder()
                    .toolUseId(toolUse.toolUseId())
                    .content(ToolResultContentBlock.builder().text(toolResultText(result)).build())
                    .status(result.success() ? "success" : "error")
                    .build()));
        }
        return Message.builder().role(ConversationRole.USER).content(toolResults).build();
    }

    private String toolResultText(final AgentToolResult result) {
        if (result.success()) {
            return result.resultJson() != null ? result.resultJson() : "{}";
        }
        return "{\"error\":\"" + (result.errorMessage() != null
                ? result.errorMessage().replace("\"", "'") : "tool execution failed") + "\"}";
    }

    private String extractText(final Message message) {
        if (message == null || message.content() == null) {
            return "";
        }
        final StringBuilder text = new StringBuilder();
        for (final ContentBlock block : message.content()) {
            if (block.text() != null) {
                text.append(block.text());
            }
        }
        return text.toString();
    }

    private String documentToJson(final Document input) {
        if (input == null) {
            return "{}";
        }
        try {
            return JSON.writeValueAsString(toPlainObject(input));
        } catch (final Exception e) {
            log.warn("Failed to serialize tool input document: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Recursively converts an AWS SDK {@link Document} into plain Java types so Jackson can
     * serialize it to the JSON string the dispatcher parses. Numbers are kept as BigDecimal so
     * integers stay integral (e.g. {@code 1}, not {@code 1.0}).
     */
    private Object toPlainObject(final Document document) {
        if (document == null || document.isNull()) {
            return null;
        }
        if (document.isMap()) {
            final Map<String, Object> map = new LinkedHashMap<>();
            document.asMap().forEach((key, value) -> map.put(key, toPlainObject(value)));
            return map;
        }
        if (document.isList()) {
            return document.asList().stream().map(this::toPlainObject).toList();
        }
        if (document.isBoolean()) {
            return document.asBoolean();
        }
        if (document.isNumber()) {
            return new BigDecimal(document.asNumber().stringValue());
        }
        return document.asString();
    }
}

