package com.stockops.repository;

import com.stockops.entity.ControllerCommand;
import com.stockops.entity.ControllerCommandResultStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for controller command history.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface ControllerCommandRepository extends JpaRepository<ControllerCommand, Long> {

    List<ControllerCommand> findByControllerIdOrderByCreatedAtDesc(Long controllerId, Pageable pageable);

    Optional<ControllerCommand> findByCorrelationId(String correlationId);

    List<ControllerCommand> findByResultStatusAndCreatedAtBefore(
            ControllerCommandResultStatus resultStatus, Instant cutoff);
}
