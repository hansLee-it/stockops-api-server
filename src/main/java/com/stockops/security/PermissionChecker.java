package com.stockops.security;

import com.stockops.dto.ExcelEntityType;
import com.stockops.repository.WarehouseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Evaluates permission-based access checks for method security expressions.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component("permissionChecker")
public class PermissionChecker {

    private final WarehouseRepository warehouseRepository;

    /**
     * Returns whether the current user has the requested permission.
     *
     * @param permission permission code
     * @return {@code true} when granted
     */
    public boolean hasPermission(final String permission) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || permission.equals(authority.getAuthority()));
    }

    /**
     * Returns whether the current user has any of the requested permissions.
     *
     * @param permissions permission codes
     * @return {@code true} when at least one permission is granted
     */
    public boolean hasAnyPermission(final String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPermission);
    }

    /**
     * Returns whether the current user can access the requested center.
     *
     * @param centerId center identifier
     * @return {@code true} when the center is in scope
     */
    public boolean hasCenterScope(final Long centerId) {
        return currentScopeProfile().canAccessCenter(centerId);
    }

    /**
     * Returns whether the current user can access the requested warehouse.
     *
     * @param warehouseId warehouse identifier
     * @return {@code true} when the warehouse is in scope
     */
    public boolean hasWarehouseScope(final Long warehouseId) {
        if (warehouseId == null) {
            return true;
        }

        final ScopeAccessProfile profile = currentScopeProfile();
        if (profile.global() || profile.warehouseIds().contains(warehouseId)) {
            return true;
        }

        return warehouseRepository.findById(warehouseId)
                .map(warehouse -> warehouse.getCenter() != null && profile.centerIds().contains(warehouse.getCenter().getId()))
                .orElse(false);
    }

    /**
     * Returns whether the current user has the permission and center scope.
     *
     * @param permission permission code
     * @param centerId center identifier
     * @return {@code true} when both checks pass
     */
    public boolean hasPermissionForCenter(final String permission, final Long centerId) {
        return hasPermission(permission) && hasCenterScope(centerId);
    }

    /**
     * Returns whether the current user has the permission and warehouse scope.
     *
     * @param permission permission code
     * @param warehouseId warehouse identifier
     * @return {@code true} when both checks pass
     */
    public boolean hasPermissionForWarehouse(final String permission, final Long warehouseId) {
        return hasPermission(permission) && hasWarehouseScope(warehouseId);
    }

    /**
     * Returns whether the current user can download the requested Excel template.
     * Template permissions stay permission-based while remaining aligned with each entity's read surface.
     *
     * @param entityTypePath excel entity path segment
     * @return {@code true} when the template is allowed
     */
    public boolean hasExcelTemplatePermission(final String entityTypePath) {
        final ExcelEntityType entityType = resolveExcelEntityType(entityTypePath);
        if (entityType == null) {
            return false;
        }

        return switch (entityType) {
            case PRODUCTS -> hasAnyPermission("PRODUCT_READ", "PRODUCT_CREATE", "PRODUCT_UPDATE");
            case INBOUNDS -> hasAnyPermission("INBOUND_READ", "INBOUND_CREATE", "INBOUND_CONFIRM");
            case PURCHASE_ORDERS -> hasAnyPermission("PURCHASE_ORDER_READ", "PURCHASE_ORDER_CREATE", "PURCHASE_ORDER_MANAGE");
        };
    }

    /**
     * Returns whether the current user can import the requested Excel workbook type.
     *
     * @param entityTypePath excel entity path segment
     * @return {@code true} when the import is allowed
     */
    public boolean hasExcelImportPermission(final String entityTypePath) {
        final ExcelEntityType entityType = resolveExcelEntityType(entityTypePath);
        if (entityType == null) {
            return false;
        }

        return switch (entityType) {
            case PRODUCTS -> hasPermission("PRODUCT_CREATE");
            case INBOUNDS -> hasPermission("INBOUND_CREATE");
            case PURCHASE_ORDERS -> hasPermission("PURCHASE_ORDER_CREATE");
        };
    }

    private ScopeAccessProfile currentScopeProfile() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ScopedUserDetails userDetails)) {
            return new ScopeAccessProfile(true, java.util.List.of(ScopeAssignment.global()), java.util.Set.of(), java.util.Set.of());
        }
        return userDetails.getScopeAccessProfile();
    }

    private ExcelEntityType resolveExcelEntityType(final String entityTypePath) {
        try {
            return ExcelEntityType.fromPathValue(entityTypePath);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public PermissionChecker(final WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

}
