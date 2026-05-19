package com.stockops.ai.provider.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Gemini external AI provider.
 *
 * @author StockOps Team
 * @since 2.0
 * @see GeminiAiProvider
 */
@ConfigurationProperties(prefix = "stockops.ai.gemini")
public class GeminiAiProperties {

    private String apiKey = "";

    private boolean enabled = false;

    private String modelName = "gemini-pro";

    private int maxTokens = 1024;

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    public int getMaxTokens() {
        return this.maxTokens;
    }

    public void setMaxTokens(final int maxTokens) {
        this.maxTokens = maxTokens;
    }
}
