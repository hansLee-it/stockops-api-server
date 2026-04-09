package com.stockops.repository;

import com.stockops.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for role master data.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a role by its unique name.
     *
     * @param name role name
     * @return matching role when present
     */
    Optional<Role> findByName(String name);
}
