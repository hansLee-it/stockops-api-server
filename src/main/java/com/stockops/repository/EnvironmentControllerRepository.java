package com.stockops.repository;

import com.stockops.entity.EnvironmentController;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for environment controller masters.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface EnvironmentControllerRepository extends JpaRepository<EnvironmentController, Long> {

    Optional<EnvironmentController> findByIdAndDeletedFalse(Long id);

    Optional<EnvironmentController> findByExternalControllerIdAndDeletedFalse(String externalControllerId);

    Optional<EnvironmentController> findByExternalControllerId(String externalControllerId);

    Page<EnvironmentController> findAllByDeletedFalse(Pageable pageable);

    boolean existsByExternalControllerIdAndDeletedFalse(String externalControllerId);
}
