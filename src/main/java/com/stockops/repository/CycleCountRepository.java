package com.stockops.repository;

import com.stockops.entity.CycleCount;
import com.stockops.entity.CycleCountStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for cycle count headers.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface CycleCountRepository extends JpaRepository<CycleCount, Long> {

    /**
     * Counts cycle counts that match any of the supplied workflow states.
     *
     * @param statuses workflow states to include
     * @return matching cycle count total
     */
    long countByStatusIn(Collection<CycleCountStatus> statuses);
}
