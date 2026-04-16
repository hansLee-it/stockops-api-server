package com.stockops.environment.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Time conversion utilities for environment monitoring.
 * Database timestamps remain UTC via {@link Instant}; KST conversion is applied only for display and KST date-range queries.
 *
 * @author StockOps Team
 * @since 1.0
 */
public final class TimeUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (z)");

    private TimeUtils() {
    }

    /**
     * Converts a UTC instant to a KST zoned timestamp for presentation.
     *
     * @param utc UTC instant from persistence
     * @return KST zoned timestamp or {@code null} when the input is {@code null}
     */
    public static ZonedDateTime toKst(final Instant utc) {
        return utc == null ? null : utc.atZone(KST);
    }

    /**
     * Converts the start of a KST business date into a UTC instant for querying.
     *
     * @param kstDate business date in KST
     * @return UTC instant representing 00:00:00 KST
     */
    public static Instant kstDateStartUtc(final LocalDate kstDate) {
        return Objects.requireNonNull(kstDate, "kstDate must not be null")
                .atStartOfDay(KST)
                .toInstant();
    }

    /**
     * Converts the end of a KST business date into a UTC instant for querying.
     *
     * @param kstDate business date in KST
     * @return UTC instant representing 23:59:59.999999999 KST
     */
    public static Instant kstDateEndUtc(final LocalDate kstDate) {
        return Objects.requireNonNull(kstDate, "kstDate must not be null")
                .plusDays(1)
                .atStartOfDay(KST)
                .minusNanos(1)
                .toInstant();
    }

    /**
     * Formats a UTC instant for KST display.
     *
     * @param utc UTC instant from persistence
     * @return formatted KST timestamp or {@code null} when the input is {@code null}
     */
    public static String formatKst(final Instant utc) {
        return utc == null ? null : toKst(utc).format(KST_FORMATTER);
    }
}
