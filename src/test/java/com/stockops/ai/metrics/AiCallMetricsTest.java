package com.stockops.ai.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiCallMetricsTest {

    private MeterRegistry meterRegistry;
    private AiCallMetrics aiCallMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        aiCallMetrics = new AiCallMetrics(meterRegistry);
    }

    @Test
    void record_successfulBedrockCall_incrementsCounterAndRecordsTimer() {
        final AiCallRecord record = new AiCallRecord(
                "req-001", "bedrock", "anthropic.claude-3-haiku", "RECOMMENDATION_EXPLANATION",
                true, false, null, 350L, 120, 85, Instant.now());

        aiCallMetrics.record(record);

        final Counter counter = meterRegistry.counter(
                "ai.bedrock.requests",
                "provider", "bedrock",
                "useCase", "RECOMMENDATION_EXPLANATION",
                "success", "true",
                "fallback", "false");
        assertThat(counter.count()).isEqualTo(1.0);

        final Timer timer = meterRegistry.timer(
                "ai.bedrock.latency",
                "provider", "bedrock",
                "useCase", "RECOMMENDATION_EXPLANATION");
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(350.0);

        final io.micrometer.core.instrument.Counter inputTokenCounter = meterRegistry.counter(
                "ai.bedrock.tokens",
                "provider", "bedrock",
                "useCase", "RECOMMENDATION_EXPLANATION",
                "direction", "input");
        assertThat(inputTokenCounter.count()).isEqualTo(120.0);

        final io.micrometer.core.instrument.Counter outputTokenCounter = meterRegistry.counter(
                "ai.bedrock.tokens",
                "provider", "bedrock",
                "useCase", "RECOMMENDATION_EXPLANATION",
                "direction", "output");
        assertThat(outputTokenCounter.count()).isEqualTo(85.0);
    }

    @Test
    void record_fallbackVertexCall_tagsFallbackTrue() {
        final AiCallRecord record = new AiCallRecord(
                "req-002", "vertex", "gemini-2.5-flash", "OPS_SUMMARY",
                true, true, null, 800L, null, null, Instant.now());

        aiCallMetrics.record(record);

        final Counter counter = meterRegistry.counter(
                "ai.bedrock.requests",
                "provider", "vertex",
                "useCase", "OPS_SUMMARY",
                "success", "true",
                "fallback", "true");
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void record_failedBedrockCall_tagsSuccessFalse() {
        final AiCallRecord record = new AiCallRecord(
                "req-003", "bedrock", "", "RECOMMENDATION_EXPLANATION",
                false, false, "timeout exceeded", 15000L, null, null, Instant.now());

        aiCallMetrics.record(record);

        final Counter counter = meterRegistry.counter(
                "ai.bedrock.requests",
                "provider", "bedrock",
                "useCase", "RECOMMENDATION_EXPLANATION",
                "success", "false",
                "fallback", "false");
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void record_nullTokenCounts_doesNotRegisterTokenCounter() {
        final AiCallRecord record = new AiCallRecord(
                "req-004", "vertex", "gemini-2.5-flash", "OPS_SUMMARY",
                true, true, null, 600L, null, null, Instant.now());

        aiCallMetrics.record(record);

        // Token counter must NOT be registered when tokens are null
        assertThat(meterRegistry.find("ai.bedrock.tokens").counters()).isEmpty();
    }

    @Test
    void record_multipleCallsSameUseCase_accumulatesCounterAndTimer() {
        for (int i = 0; i < 5; i++) {
            aiCallMetrics.record(new AiCallRecord(
                    "req-" + i, "bedrock", "model-id", "RAG_QUERY",
                    true, false, null, 100L, 50, 30, Instant.now()));
        }

        final Counter counter = meterRegistry.counter(
                "ai.bedrock.requests",
                "provider", "bedrock",
                "useCase", "RAG_QUERY",
                "success", "true",
                "fallback", "false");
        assertThat(counter.count()).isEqualTo(5.0);

        final Timer timer = meterRegistry.timer(
                "ai.bedrock.latency",
                "provider", "bedrock",
                "useCase", "RAG_QUERY");
        assertThat(timer.count()).isEqualTo(5L);
    }
}
