package com.stockops.ai.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Records AI provider call metrics as structured audit logs and Micrometer counters/timers.
 *
 * <p>Emits four metric families:
 * <ul>
 *   <li>{@code ai.bedrock.requests} — counter tagged by provider, useCase, success, fallback</li>
 *   <li>{@code ai.bedrock.latency} — timer tagged by provider, useCase</li>
 *   <li>{@code ai.bedrock.tokens} — counter tagged by provider, useCase, direction (input/output);
 *       only incremented when token counts are available in the provider response</li>
 *   <li>Structured log line via logger {@code ai.call.audit}</li>
 * </ul>
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
public class AiCallMetrics {

    private static final Logger AI_CALL_LOG = LoggerFactory.getLogger("ai.call.audit");

    private final MeterRegistry meterRegistry;

    public AiCallMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records a completed AI call — both structured audit log and Micrometer metrics.
     *
     * @param record call snapshot to record
     */
    public void record(final AiCallRecord record) {
        AI_CALL_LOG.info(
                "AI_CALL requestId={} provider={} modelId={} useCase={} success={} fallback={} latencyMs={} inputTokens={} outputTokens={} failureReason={}",
                record.requestId(),
                record.provider(),
                record.modelId(),
                record.useCase(),
                record.success(),
                record.fallbackUsed(),
                record.latencyMs(),
                record.inputTokens(),
                record.outputTokens(),
                record.failureReason());

        meterRegistry.counter(
                        "ai.bedrock.requests",
                        "provider", record.provider(),
                        "useCase", record.useCase(),
                        "success", String.valueOf(record.success()),
                        "fallback", String.valueOf(record.fallbackUsed()))
                .increment();

        meterRegistry.timer(
                        "ai.bedrock.latency",
                        "provider", record.provider(),
                        "useCase", record.useCase())
                .record(Duration.ofMillis(record.latencyMs()));

        if (record.inputTokens() != null) {
            meterRegistry.counter(
                            "ai.bedrock.tokens",
                            "provider", record.provider(),
                            "useCase", record.useCase(),
                            "direction", "input")
                    .increment(record.inputTokens());
        }
        if (record.outputTokens() != null) {
            meterRegistry.counter(
                            "ai.bedrock.tokens",
                            "provider", record.provider(),
                            "useCase", record.useCase(),
                            "direction", "output")
                    .increment(record.outputTokens());
        }
    }
}
