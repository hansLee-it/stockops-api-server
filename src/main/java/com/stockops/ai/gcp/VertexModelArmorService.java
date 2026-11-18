package com.stockops.ai.gcp;

import com.google.cloud.modelarmor.v1.DataItem;
import com.google.cloud.modelarmor.v1.ModelArmorClient;
import com.google.cloud.modelarmor.v1.ModelArmorSettings;
import com.google.cloud.modelarmor.v1.SanitizeModelResponseRequest;
import com.google.cloud.modelarmor.v1.SanitizeModelResponseResponse;
import com.google.cloud.modelarmor.v1.SanitizeUserPromptRequest;
import com.google.cloud.modelarmor.v1.SanitizeUserPromptResponse;
import com.google.cloud.modelarmor.v1.SanitizationResult;
import com.google.cloud.modelarmor.v1.TemplateName;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VertexModelArmorService {

    private static final Logger log = LoggerFactory.getLogger(VertexModelArmorService.class);

    private final VertexAiProperties properties;

    public VertexModelArmorService(final VertexAiProperties properties) {
        this.properties = properties;
    }

    public void sanitizeUserPrompt(final String userPrompt) {
        final VertexAiProperties.ModelArmor modelArmor = properties.getModelArmor();
        if (modelArmor == null || !modelArmor.hasInputTemplate() || userPrompt == null || userPrompt.isBlank()) {
            return;
        }

        sanitize("prompt", modelArmor.getInputTemplateId(), () -> {
            try (ModelArmorClient client = createClient(modelArmor.getLocation())) {
                final SanitizeUserPromptRequest request = SanitizeUserPromptRequest.newBuilder()
                        .setName(templateName(modelArmor, modelArmor.getInputTemplateId()))
                        .setUserPromptData(DataItem.newBuilder().setText(userPrompt).build())
                        .build();
                final SanitizeUserPromptResponse response = client.sanitizeUserPrompt(request);
                return response.getSanitizationResult();
            }
        });
    }

    public void sanitizeModelResponse(final String modelResponse) {
        final VertexAiProperties.ModelArmor modelArmor = properties.getModelArmor();
        if (modelArmor == null || !modelArmor.hasOutputTemplate() || modelResponse == null || modelResponse.isBlank()) {
            return;
        }

        sanitize("response", modelArmor.getOutputTemplateId(), () -> {
            try (ModelArmorClient client = createClient(modelArmor.getLocation())) {
                final SanitizeModelResponseRequest request = SanitizeModelResponseRequest.newBuilder()
                        .setName(templateName(modelArmor, modelArmor.getOutputTemplateId()))
                        .setModelResponseData(DataItem.newBuilder().setText(modelResponse).build())
                        .build();
                final SanitizeModelResponseResponse response = client.sanitizeModelResponse(request);
                return response.getSanitizationResult();
            }
        });
    }

    private void sanitize(final String direction,
                          final String templateId,
                          final SanitizationCall call) {
        final VertexAiProperties.ModelArmor modelArmor = properties.getModelArmor();
        try {
            final SanitizationResult result = call.execute();
            final boolean matchFound = isMatchFound(result);
            if (matchFound && modelArmor.shouldBlockMatches()) {
                throw new VertexModelArmorBlockedException(blockMessage(modelArmor, direction));
            }
            if (matchFound) {
                log.warn("[Vertex Model Armor] {} matched template={} mode={}",
                        direction, templateId, modelArmor.getEnforcementMode());
            } else {
                log.debug("[Vertex Model Armor] {} passed template={}", direction, templateId);
            }
        } catch (final VertexModelArmorBlockedException e) {
            throw e;
        } catch (final RuntimeException | IOException e) {
            if (modelArmor.isFailOpen()) {
                log.warn("[Vertex Model Armor] {} check failed open for template={}: {}",
                        direction, templateId, e.getMessage());
                return;
            }
            throw new IllegalStateException("Vertex Model Armor " + direction + " check failed", e);
        }
    }

    protected ModelArmorClient createClient(final String location) throws IOException {
        final String apiEndpoint = String.format(Locale.ROOT, "modelarmor.%s.rep.googleapis.com:443", location);
        final ModelArmorSettings settings = ModelArmorSettings.newBuilder()
                .setEndpoint(apiEndpoint)
                .build();
        return ModelArmorClient.create(settings);
    }

    private static String templateName(final VertexAiProperties.ModelArmor modelArmor,
                                       final String templateId) {
        return TemplateName.of(modelArmor.getProjectId(), modelArmor.getLocation(), templateId).toString();
    }

    private static boolean isMatchFound(final SanitizationResult result) {
        return result != null
                && result.getFilterMatchState() != null
                && "MATCH_FOUND".equals(result.getFilterMatchState().name());
    }

    private static String blockMessage(final VertexAiProperties.ModelArmor modelArmor,
                                       final String direction) {
        if (modelArmor.getBlockMessage() != null && !modelArmor.getBlockMessage().isBlank()) {
            return modelArmor.getBlockMessage();
        }
        return "Vertex AI fallback " + direction + " blocked by Model Armor.";
    }

    @FunctionalInterface
    private interface SanitizationCall {
        SanitizationResult execute() throws IOException;
    }
}
