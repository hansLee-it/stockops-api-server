package com.stockops.repository;

import com.stockops.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted audit logs.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
