package com.stockops.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the {@link io.micrometer.observation.annotation.Observed @Observed}
 * annotation as a span/observation boundary.
 *
 * <p>Spring Boot auto-configures an {@link ObservationRegistry} (bridged to the
 * OpenTelemetry tracer by {@code micrometer-tracing-bridge-otel}) but does
 * <em>not</em> register the AOP aspect that turns {@code @Observed}-annotated
 * methods into observations. Without this bean the annotation is silently
 * ignored. Registering {@link ObservedAspect} (weaved via
 * {@code spring-boot-starter-aop}) makes each annotated service/client method
 * appear as a nested child span under the auto-instrumented HTTP server span,
 * so the in-process call flow is visible in the trace waterfall — not just the
 * request/response envelope.
 *
 * @author StockOps Team
 * @since 2.1
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Registers the {@code @Observed} AOP aspect against the auto-configured
     * observation registry.
     *
     * @param observationRegistry Spring-managed registry wired to the OTel tracer
     * @return the aspect that intercepts {@code @Observed}-annotated methods
     */
    @Bean
    public ObservedAspect observedAspect(final ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
