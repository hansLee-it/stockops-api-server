package com.stockops.repository;

import com.stockops.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for permission master data.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Finds a permission by its unique code.
     *
     * @param code permission code
     * @return matching permission when present
     */
    Optional<Permission> findByCode(String code);
}
