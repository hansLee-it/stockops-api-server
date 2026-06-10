package com.stockops.ai.gcp;

import com.stockops.ai.provider.AiGenerationRequest;
import com.stockops.ai.provider.AiGenerationResponse;
import com.stockops.ai.provider.AiServiceStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VertexAiGenerationProviderTest {

    @Mock VertexAiProperties properties;

    VertexAiGenerationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new VertexAiGenerationProvider(properties);
    }

    @Test
    void generate_throwsWhenProviderDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> provider.generate(sampleRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void generate_throwsWhenProjectIdNotConfigured() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProjectId()).thenReturn(null);

        assertThatThrownBy(() -> provider.generate(sampleRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("project id");
    }

    @Test
    void generate_extractsTokenCountsFromUsageMetadata() {
        // Stub via spy — avoids actual network call to Vertex AI
        final VertexAiGenerationProvider spy = spy(provider);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProjectId()).thenReturn("test-project");
        when(properties.getModelId()).thenReturn("gemini-1.5-pro");
        // Note: sampleRequest() has chatVisible=false, so getFallbackNotice() is NOT called

        final com.google.genai.types.GenerateContentResponseUsageMetadata mockMeta =
                mock(com.google.genai.types.GenerateContentResponseUsageMetadata.class);
        when(mockMeta.promptTokenCount()).thenReturn(Optional.of(120));
        when(mockMeta.candidatesTokenCount()).thenReturn(Optional.of(80));

        final com.google.genai.types.GenerateContentResponse mockResponse =
                mock(com.google.genai.types.GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("AI 운영 요약 응답");
        when(mockResponse.usageMetadata()).thenReturn(Optional.of(mockMeta));

        doReturn(mockResponse).when(spy).callApi(anyString(), anyString());

        final AiGenerationResponse result = spy.generate(sampleRequest());

        assertThat(result.text()).isEqualTo("AI 운영 요약 응답");
        assertThat(result.inputTokens()).isEqualTo(120);
        assertThat(result.outputTokens()).isEqualTo(80);
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.serviceStatus()).isEqualTo(AiServiceStatus.FALLBACK_ACTIVE);
        assertThat(result.provider()).isEqualTo("vertex-ai");
        assertThat(result.fallbackNotice()).isEmpty(); // chatVisible=false → no notice
    }

    @Test
    void generate_handlesAbsentUsageMetadataGracefully() {
        // Vertex AI may not always return token counts — should not fail
        final VertexAiGenerationProvider spy = spy(provider);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProjectId()).thenReturn("test-project");
        when(properties.getModelId()).thenReturn("gemini-1.5-pro");
        // Note: sampleRequest() has chatVisible=false, so getFallbackNotice() is NOT called

        final com.google.genai.types.GenerateContentResponse mockResponse =
                mock(com.google.genai.types.GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("응답 텍스트");
        when(mockResponse.usageMetadata()).thenReturn(Optional.empty());

        doReturn(mockResponse).when(spy).callApi(anyString(), anyString());

        final AiGenerationResponse result = spy.generate(sampleRequest());

        assertThat(result.text()).isEqualTo("응답 텍스트");
        assertThat(result.inputTokens()).isNull();
        assertThat(result.outputTokens()).isNull();
    }

    @Test
    void generate_throwsWhenResponseTextIsBlank() {
        final VertexAiGenerationProvider spy = spy(provider);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProjectId()).thenReturn("test-project");
        when(properties.getModelId()).thenReturn("gemini-1.5-pro");
        // Note: code throws before reaching usageMetadata extraction — do not stub it

        final com.google.genai.types.GenerateContentResponse mockResponse =
                mock(com.google.genai.types.GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("");

        doReturn(mockResponse).when(spy).callApi(anyString(), anyString());

        assertThatThrownBy(() -> spy.generate(sampleRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void generate_includesFallbackNoticeOnlyForChatVisibleRequests() {
        final VertexAiGenerationProvider spy = spy(provider);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProjectId()).thenReturn("test-project");
        when(properties.getModelId()).thenReturn("gemini-1.5-pro");
        when(properties.getFallbackNotice()).thenReturn("기본 제공 모델의 연결이 불안정합니다.");

        final com.google.genai.types.GenerateContentResponse mockResponse =
                mock(com.google.genai.types.GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("응답");
        when(mockResponse.usageMetadata()).thenReturn(Optional.empty());
        doReturn(mockResponse).when(spy).callApi(anyString(), anyString());

        // Chat-visible request → notice populated
        final AiGenerationResponse chatResult = spy.generate(
                new AiGenerationRequest("sys", "user", "CHAT", true));
        assertThat(chatResult.fallbackNotice()).isEqualTo("기본 제공 모델의 연결이 불안정합니다.");

        // Non-chat request → notice empty (getFallbackNotice() not called for this)
        final AiGenerationResponse nonChatResult = spy.generate(
                new AiGenerationRequest("sys", "user", "EXPLANATION", false));
        assertThat(nonChatResult.fallbackNotice()).isEmpty();
    }

    private AiGenerationRequest sampleRequest() {
        return new AiGenerationRequest("You are a helpful assistant.", "현재 재고 상태를 요약해줘.", "OPS_SUMMARY", false);
    }
}
