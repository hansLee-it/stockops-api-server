package com.stockops.environment.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-op recent reading cache used when Redis is disabled
 * ({@code stockops.redis.enabled=false}, e.g. local profile and tests).
 * Writes are dropped and reads return empty results, matching the API contract
 * of "no recent readings available".
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
@ConditionalOnProperty(name = "stockops.redis.enabled", havingValue = "false")
public class NoOpSensorReadingCacheService implements SensorReadingCacheService {

    @Override
    public void append(final SensorReadingSnapshot snapshot) {
        // intentionally empty — readings are only cached when Redis is enabled
    }

    @Override
    public List<SensorReadingSnapshot> readRecent(final Long sensorDeviceId, final Duration window) {
        return List.of();
    }

    @Override
    public Optional<SensorReadingSnapshot> latest(final Long sensorDeviceId) {
        return Optional.empty();
    }
}
