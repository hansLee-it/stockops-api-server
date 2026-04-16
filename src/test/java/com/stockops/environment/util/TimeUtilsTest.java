package com.stockops.environment.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TimeUtils}.
 *
 * @author StockOps Team
 * @since 1.0
 */
class TimeUtilsTest {

    /**
     * Verifies that UTC instants are converted to KST without changing the underlying moment.
     */
    @Test
    void toKstConvertsUtcInstantToAsiaSeoulZone() {
        final Instant utc = Instant.parse("2026-04-15T00:00:00Z");

        assertThat(TimeUtils.toKst(utc))
                .isNotNull()
                .satisfies(kst -> {
                    assertThat(kst.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
                    assertThat(kst.getHour()).isEqualTo(9);
                    assertThat(kst.toInstant()).isEqualTo(utc);
                });
    }

    /**
     * Verifies that KST date boundaries are translated into the correct UTC query range.
     */
    @Test
    void kstDateBoundariesConvertToExpectedUtcInstants() {
        final LocalDate kstDate = LocalDate.of(2026, 4, 15);

        assertThat(TimeUtils.kstDateStartUtc(kstDate)).isEqualTo(Instant.parse("2026-04-14T15:00:00Z"));
        assertThat(TimeUtils.kstDateEndUtc(kstDate)).isEqualTo(Instant.parse("2026-04-15T14:59:59.999999999Z"));
    }

    /**
     * Verifies that display formatting emits KST text while keeping null handling safe.
     */
    @Test
    void formatKstFormatsDisplayAndHandlesNull() {
        assertThat(TimeUtils.formatKst(Instant.parse("2026-04-15T00:00:00Z")))
                .isEqualTo("2026-04-15 09:00:00 (KST)");
        assertThat(TimeUtils.formatKst(null)).isNull();
        assertThat(TimeUtils.toKst(null)).isNull();
    }
}
