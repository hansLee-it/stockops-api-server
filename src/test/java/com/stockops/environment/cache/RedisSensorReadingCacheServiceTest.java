package com.stockops.environment.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Unit tests for {@link RedisSensorReadingCacheService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class RedisSensorReadingCacheServiceTest {

    private static final String KEY = "stockops:sensor:readings:5";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisSensorReadingCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        cacheService = new RedisSensorReadingCacheService(redisTemplate, new SensorCacheProperties());
    }

    /**
     * Verifies a reading is added with its recordedAt epoch-millis score,
     * expired entries are trimmed, and the key TTL is refreshed.
     */
    @Test
    void appendWritesTrimsAndRefreshesTtl() {
        final Instant recordedAt = Instant.parse("2026-06-11T09:15:30Z");

        cacheService.append(snapshot(recordedAt));

        final ArgumentCaptor<String> memberCaptor = ArgumentCaptor.forClass(String.class);
        verify(zSetOperations).add(eq(KEY), memberCaptor.capture(), eq((double) recordedAt.toEpochMilli()));
        assertThat(memberCaptor.getValue()).contains("\"sensorDeviceId\":5").contains("\"value\":23.4");
        verify(zSetOperations).removeRangeByScore(eq(KEY), eq(0.0), anyDouble());
        verify(redisTemplate).expire(KEY, Duration.ofMinutes(15));
    }

    /**
     * Verifies Redis write failures are swallowed so ingestion never breaks.
     */
    @Test
    void appendSwallowsRedisFailures() {
        when(zSetOperations.add(anyString(), anyString(), anyDouble()))
                .thenThrow(new IllegalStateException("redis down"));

        cacheService.append(snapshot(Instant.parse("2026-06-11T09:15:30Z")));
    }

    /**
     * Verifies recent readings are deserialized from the requested score window.
     */
    @Test
    void readRecentReturnsDeserializedWindow() {
        final Set<String> members = new LinkedHashSet<>();
        members.add("{\"sensorDeviceId\":5,\"siteId\":\"site-a\",\"sensorId\":\"temp-001\","
                + "\"sensorType\":\"TEMPERATURE\",\"valueKind\":\"temperature\",\"value\":23.4,"
                + "\"unit\":\"C\",\"status\":\"NORMAL\",\"recordedAt\":\"2026-06-11T09:15:30Z\","
                + "\"sequenceId\":184}");
        members.add("not-json");
        when(zSetOperations.rangeByScore(eq(KEY), anyDouble(), anyDouble())).thenReturn(members);

        final List<SensorReadingSnapshot> readings = cacheService.readRecent(5L, Duration.ofMinutes(10));

        assertThat(readings).hasSize(1);
        assertThat(readings.get(0).value()).isEqualTo(23.4);
        assertThat(readings.get(0).recordedAt()).isEqualTo(Instant.parse("2026-06-11T09:15:30Z"));
        assertThat(readings.get(0).sequenceId()).isEqualTo(184L);
    }

    /**
     * Verifies read failures return an empty list instead of propagating.
     */
    @Test
    void readRecentReturnsEmptyOnRedisFailure() {
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenThrow(new IllegalStateException("redis down"));

        assertThat(cacheService.readRecent(5L, Duration.ofMinutes(10))).isEmpty();
    }

    /**
     * Verifies the latest reading comes from the reverse range head.
     */
    @Test
    void latestReturnsNewestCachedReading() {
        when(zSetOperations.reverseRange(KEY, 0, 0)).thenReturn(Set.of(
                "{\"sensorDeviceId\":5,\"siteId\":\"site-a\",\"sensorId\":\"temp-001\","
                        + "\"sensorType\":\"TEMPERATURE\",\"valueKind\":\"temperature\",\"value\":25.0,"
                        + "\"unit\":\"C\",\"status\":\"WARNING\",\"recordedAt\":\"2026-06-11T09:20:00Z\","
                        + "\"sequenceId\":200}"));

        final Optional<SensorReadingSnapshot> latest = cacheService.latest(5L);

        assertThat(latest).isPresent();
        assertThat(latest.get().status()).isEqualTo("WARNING");
        assertThat(latest.get().value()).isEqualTo(25.0);
    }

    /**
     * Verifies a missing key yields an empty latest reading.
     */
    @Test
    void latestReturnsEmptyWhenNothingCached() {
        when(zSetOperations.reverseRange(KEY, 0, 0)).thenReturn(Set.of());

        assertThat(cacheService.latest(5L)).isEmpty();
    }

    private SensorReadingSnapshot snapshot(final Instant recordedAt) {
        return new SensorReadingSnapshot(5L, "site-a", "temp-001", "TEMPERATURE", "temperature",
                23.4, "C", "NORMAL", recordedAt, 184L);
    }
}
