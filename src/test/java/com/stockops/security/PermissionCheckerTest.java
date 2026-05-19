package com.stockops.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.stockops.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link PermissionChecker}.
 */
class PermissionCheckerTest {

    private final PermissionChecker permissionChecker = new PermissionChecker(mock(WarehouseRepository.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasPermissionAllowsAdminRoleAsRoot() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("admin@stockops.com", "n/a", "ROLE_ADMIN"));

        assertThat(permissionChecker.hasPermission("ANY_PERMISSION")).isTrue();
    }

    @Test
    void hasPermissionRequiresMatchingPermissionForNonAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("worker@stockops.com", "n/a", "INVENTORY_READ"));

        assertThat(permissionChecker.hasPermission("INVENTORY_READ")).isTrue();
        assertThat(permissionChecker.hasPermission("CYCLE_COUNT_READ")).isFalse();
    }
}
