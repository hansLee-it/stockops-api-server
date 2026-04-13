package com.stockops.repository;

import com.stockops.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for role-to-permission mappings.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * Returns all permission codes assigned to the supplied role.
     *
     * @param roleId role identifier
     * @return granted permission codes
     */
    @Query("""
            select rp.permission.code
            from RolePermission rp
            where rp.role.id = :roleId
            order by rp.permission.code asc
            """)
    List<String> findPermissionCodesByRoleId(@Param("roleId") Long roleId);
}
