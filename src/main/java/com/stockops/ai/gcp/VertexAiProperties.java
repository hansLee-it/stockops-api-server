package com.stockops.ai.gcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stockops.ai.vertex")
public class VertexAiProperties {

    private boolean enabled;
    private String projectId = "";
    private String location = "us-central1";
    private String modelId = "gemini-2.5-flash";
    private String credentialsJson = "";
    private String fallbackNotice = "기본 제공 모델의 연결이 불안정하여 보조 시스템으로 우회합니다.";
    private String noServiceNotice = "연동된 AI서비스가 없습니다.";
    private String unauthenticatedNotice = "인증된 AI서비스가 없습니다.";
    private ModelArmor modelArmor = new ModelArmor();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getCredentialsJson() { return credentialsJson; }
    public void setCredentialsJson(String credentialsJson) { this.credentialsJson = credentialsJson; }
    public String getFallbackNotice() { return fallbackNotice; }
    public void setFallbackNotice(String fallbackNotice) { this.fallbackNotice = fallbackNotice; }
    public String getNoServiceNotice() { return noServiceNotice; }
    public void setNoServiceNotice(String noServiceNotice) { this.noServiceNotice = noServiceNotice; }
    public String getUnauthenticatedNotice() { return unauthenticatedNotice; }
    public void setUnauthenticatedNotice(String unauthenticatedNotice) { this.unauthenticatedNotice = unauthenticatedNotice; }
    public ModelArmor getModelArmor() { return modelArmor; }
    public void setModelArmor(ModelArmor modelArmor) { this.modelArmor = modelArmor; }

    public static class ModelArmor {
        private boolean enabled;
        private String projectId = "";
        private String location = "us-central1";
        private String inputTemplateId = "";
        private String outputTemplateId = "";
        private EnforcementMode enforcementMode = EnforcementMode.INSPECT_ONLY;
        private boolean failOpen = true;
        private String blockMessage = "Vertex AI fallback safety policy blocked this content.";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getInputTemplateId() { return inputTemplateId; }
        public void setInputTemplateId(String inputTemplateId) { this.inputTemplateId = inputTemplateId; }
        public String getOutputTemplateId() { return outputTemplateId; }
        public void setOutputTemplateId(String outputTemplateId) { this.outputTemplateId = outputTemplateId; }
        public EnforcementMode getEnforcementMode() { return enforcementMode; }
        public void setEnforcementMode(EnforcementMode enforcementMode) { this.enforcementMode = enforcementMode; }
        public boolean isFailOpen() { return failOpen; }
        public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }
        public String getBlockMessage() { return blockMessage; }
        public void setBlockMessage(String blockMessage) { this.blockMessage = blockMessage; }

        public boolean shouldBlockMatches() {
            return EnforcementMode.INSPECT_AND_BLOCK.equals(enforcementMode);
        }

        public boolean hasInputTemplate() {
            return enabled
                    && hasProjectAndLocation()
                    && inputTemplateId != null && !inputTemplateId.isBlank();
        }

        public boolean hasOutputTemplate() {
            return enabled
                    && hasProjectAndLocation()
                    && outputTemplateId != null && !outputTemplateId.isBlank();
        }

        private boolean hasProjectAndLocation() {
            return projectId != null && !projectId.isBlank()
                    && location != null && !location.isBlank();
        }
    }

    public enum EnforcementMode {
        INSPECT_ONLY,
        INSPECT_AND_BLOCK
    }
}
