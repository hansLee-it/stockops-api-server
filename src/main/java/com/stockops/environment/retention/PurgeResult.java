package com.stockops.environment.retention;

import java.time.Duration;
import java.time.Instant;

/**
 * Summary of a retention purge execution.
 *
 * @param readingsDeleted deleted sensor reading count
 * @param alertsDeleted deleted environment alert count
 * @param cutoffDate UTC cutoff used for the purge
 * @param duration total purge execution duration
 * @author StockOps Team
 * @since 1.0
 */
public record PurgeResult(int readingsDeleted, int alertsDeleted, Instant cutoffDate, Duration duration) {
}
