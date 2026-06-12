package com.stockops.repository;

import com.stockops.entity.EnvironmentAlertNotification;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import java.util.List;

/**
 * Repository for the environment alert notification outbox.
 *
 * @author StockOps Team
 * @since 2.3
 */
public interface EnvironmentAlertNotificationRepository
        extends JpaRepository<EnvironmentAlertNotification, Long> {

    /**
     * Claims the oldest pending notifications with {@code FOR UPDATE SKIP LOCKED} so multiple
     * sender instances can poll concurrently without double-delivering. Lock timeout -2 maps to
     * SKIP LOCKED in Hibernate.
     *
     * @param pageable batch size limiter
     * @return claimed pending rows, oldest first
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("select n from EnvironmentAlertNotification n where n.status = 'PENDING' order by n.createdAt")
    List<EnvironmentAlertNotification> claimPending(Pageable pageable);

    /**
     * Deletes terminal outbox rows older than the cutoff (retention cleanup).
     *
     * @param cutoff UTC cutoff
     * @return deleted row count
     */
    @Modifying
    @Query("delete from EnvironmentAlertNotification n "
            + "where n.status <> 'PENDING' and n.createdAt < :cutoff")
    int deleteTerminalBefore(Instant cutoff);
}
