package com.stockops.ai.forecast;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the external Python AI forecast service.
 *
 * @author StockOps Team
 * @since 2.0
 * @see AiForecastClient
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "stockops.ai-service")
public class AiForecastProperties {

    private String url = "http://localhost:8000";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(5);

    private int circuitBreakerFailureThreshold = 3;

    private Duration circuitBreakerCooldown = Duration.ofSeconds(30);
}
