package com.stockops.repository;

import com.stockops.entity.EnvironmentAlert;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for append-only environment alerts.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface EnvironmentAlertRepository extends JpaRepository<EnvironmentAlert, Long> {

    Page<EnvironmentAlert> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since, Pageable pageable);

    /**
     * Returns alerts created after the provided cutoff in newest-first order.
     *
     * @param cutoff UTC cutoff timestamp
     * @return alerts sorted newest first
     */
    @Query("""
            SELECT a
              FROM EnvironmentAlert a
             WHERE a.createdAt >= :cutoff
             ORDER BY a.createdAt DESC
            """)
    List<EnvironmentAlert> findAllByCreatedAtAfterOrderByCreatedAtDesc(@Param("cutoff") Instant cutoff);

    @Query(value = "SELECT * FROM environment_alerts WHERE created_at >= :cutoff ORDER BY created_at DESC", nativeQuery = true)
    List<EnvironmentAlert> findAllAfterRetentionCutoff(@Param("cutoff") Instant cutoff);

    /**
     * Deletes retained environment alerts older than the configured cutoff timestamp.
     *
     * @param cutoff UTC cutoff timestamp
     * @return number of deleted alerts
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM EnvironmentAlert a WHERE a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
