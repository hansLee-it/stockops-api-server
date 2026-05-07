package com.stockops.service.analytics;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup schema guard for the analytics layer.
 * Verifies that required columns exist before the application begins serving traffic.
 * Fails fast with a clear error when entity/schema drift is detected.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class AnalyticsSchemaGuard implements ApplicationRunner {

    private static final String SCHEMA_ANALYTICS = "analytics";
    private static final String TABLE_DAILY_FILL_RATE_SOURCE = "daily_fill_rate_source";
    private static final String COLUMN_SHIPPED_QUANTITY = "shipped_quantity";

    private final DataSource dataSource;

    /**
     * Validates required analytics columns on startup.
     *
     * @param args application arguments
     * @throws IllegalStateException if a required column is missing
     */
    @Override
    public void run(final ApplicationArguments args) {
        log.info("Running analytics schema guard...");
        verifyColumnExists(SCHEMA_ANALYTICS, TABLE_DAILY_FILL_RATE_SOURCE, COLUMN_SHIPPED_QUANTITY);
        log.info("Analytics schema guard passed.");
    }

    /**
     * Verifies that the specified column exists in the given schema and table.
     *
     * @param schema the database schema name
     * @param table the table name
     * @param column the column name to verify
     * @throws IllegalStateException if the column does not exist or metadata lookup fails
     */
    private void verifyColumnExists(final String schema, final String table, final String column) {
        try (Connection connection = dataSource.getConnection()) {
            final DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, schema, table, column)) {
                if (!columns.next()) {
                    throw new IllegalStateException(
                            String.format(
                                    "Analytics schema mismatch: required column '%s.%s.%s' is missing. "
                                            + "Ensure Flyway migration V33 has been applied.",
                                    schema, table, column));
                }
            }
        } catch (final IllegalStateException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Analytics schema guard failed while inspecting database metadata", e);
        }
    }
}
