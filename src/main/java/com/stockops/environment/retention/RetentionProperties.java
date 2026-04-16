package com.stockops.environment.retention;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable retention settings for environment monitoring history cleanup.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "stockops.retention")
public class RetentionProperties {

    private int retentionDays = 30;

    private int batchSize = 1000;

    private boolean enabled = true;
}
