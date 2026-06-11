package com.stockops.environment.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis sorted-set implementation of the recent sensor reading cache.
 *
 * <p>Key shape: {@code {prefix}:{sensorDeviceId}} with the reading's
 * {@code recordedAt} epoch milliseconds as score and compact JSON as member.
 * Every write trims entries older than the retention window and refreshes the
 * key TTL so inactive sensors expire on their own.
 *
 * <p>All Redis failures are contained here: writes log and continue, reads
 * return empty results. Telemetry ingestion must never fail because the cache
 * is unavailable.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
@ConditionalOnProperty(name = "stockops.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisSensorReadingCacheService implements SensorReadingCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisSensorReadingCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final SensorCacheProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Creates the cache service.
     *
     * @param redisTemplate string Redis template
     * @param properties sensor cache properties
     */
    public RedisSensorReadingCacheService(
            final StringRedisTemplate redisTemplate,
            final SensorCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void append(final SensorReadingSnapshot snapshot) {
        if (snapshot == null || snapshot.sensorDeviceId() == null || snapshot.recordedAt() == null) {
            return;
        }
        final String key = keyFor(snapshot.sensorDeviceId());
        try {
            final String member = objectMapper.writeValueAsString(snapshot);
            final long score = snapshot.recordedAt().toEpochMilli();
            redisTemplate.opsForZSet().add(key, member, score);
            final long cutoff = Instant.now().minus(properties.getRecentWindow()).toEpochMilli();
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff - 1);
            redisTemplate.expire(key, properties.getKeyTtl());
        } catch (Exception exception) {
            LOGGER.warn("Failed to cache sensor reading for sensorDeviceId={}: {}",
                    snapshot.sensorDeviceId(), exception.getMessage());
        }
    }

    @Override
    public List<SensorReadingSnapshot> readRecent(final Long sensorDeviceId, final Duration window) {
        if (sensorDeviceId == null || window == null || window.isNegative() || window.isZero()) {
            return List.of();
        }
        try {
            final long now = Instant.now().toEpochMilli();
            final long from = now - window.toMillis();
            final Set<String> members = redisTemplate.opsForZSet()
                    .rangeByScore(keyFor(sensorDeviceId), from, now);
            return deserialize(members);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read recent sensor readings for sensorDeviceId={}: {}",
                    sensorDeviceId, exception.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<SensorReadingSnapshot> latest(final Long sensorDeviceId) {
        if (sensorDeviceId == null) {
            return Optional.empty();
        }
        try {
            final Set<String> members = redisTemplate.opsForZSet()
                    .reverseRange(keyFor(sensorDeviceId), 0, 0);
            final List<SensorReadingSnapshot> snapshots = deserialize(members);
            return snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.get(0));
        } catch (Exception exception) {
            LOGGER.warn("Failed to read latest sensor reading for sensorDeviceId={}: {}",
                    sensorDeviceId, exception.getMessage());
            return Optional.empty();
        }
    }

    private List<SensorReadingSnapshot> deserialize(final Set<String> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        final List<SensorReadingSnapshot> snapshots = new ArrayList<>(members.size());
        for (final String member : members) {
            try {
                snapshots.add(objectMapper.readValue(member, SensorReadingSnapshot.class));
            } catch (Exception exception) {
                LOGGER.warn("Skipping unreadable cached sensor reading: {}", exception.getMessage());
            }
        }
        return Collections.unmodifiableList(snapshots);
    }

    private String keyFor(final Long sensorDeviceId) {
        return properties.getKeyPrefix() + ":" + sensorDeviceId;
    }
}
