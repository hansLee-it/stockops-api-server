package com.stockops.security;

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
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
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
}
