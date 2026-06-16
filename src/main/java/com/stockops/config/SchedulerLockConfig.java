package com.stockops.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Enables ShedLock so cron batch jobs run on exactly one instance behind a load balancer.
 *
 * <p>Cron batch schedulers (daily recommendation, analytics refresh/backfill, intraday forecast,
 * ops-summary, expiry jobs, retention) and the fixed-delay sweeps that would otherwise double-process
 * (controller-command timeout sweep, alert escalation) carry {@code @SchedulerLock}. The environment
 * alert outbox sender is intentionally left unlocked — it claims rows with {@code FOR UPDATE SKIP LOCKED}
 * and is already safe to run on every instance concurrently.
 *
 * <p>{@code usingDbTime()} makes lock timing rely on the database clock, avoiding cross-instance
 * clock skew. The lock table is created by Flyway migration {@code V54__shedlock.sql}.
 *
 * @author StockOps Team
 * @since 2.4
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerLockConfig {

    /**
     * Builds the JDBC-backed lock provider against the application datasource.
     *
     * @param dataSource application datasource
     * @return ShedLock provider
     */
    @Bean
    public LockProvider lockProvider(final DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}
