package com.stockops.ai.provider.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Gemini external AI provider.
 *
 * @author StockOps Team
 * @since 2.0
 * @see GeminiAiProvider
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "stockops.ai.gemini")
public class GeminiAiProperties {

    private String apiKey = "";

    private boolean enabled = false;

    private String modelName = "gemini-pro";

    private int maxTokens = 1024;
}
