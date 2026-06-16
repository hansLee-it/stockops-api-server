package com.stockops.auth;

import com.stockops.entity.Role;
import com.stockops.entity.User;
import com.stockops.repository.RolePermissionRepository;
import com.stockops.repository.UserRepository;
import com.stockops.security.JwtTokenProvider;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAccessService;
import com.stockops.security.ScopeAssignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies the login response carries the user's store affiliation, which client-web uses to
 * gate the store purchase-request form (only store-bound users may submit).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private ScopeAccessService scopeAccessService;

    private AuthService authService() {
        return new AuthService(authenticationManager, userRepository, rolePermissionRepository,
                jwtTokenProvider, scopeAccessService);
    }

    private User userWithStore(final Long storeId) {
        final Role role = new Role();
        role.setId(3L);
        role.setName("STORE_MANAGER");
        final User user = new User();
        user.setId(15L);
        user.setEmail("store-manager01@stockops.com");
        user.setName("Store Manager");
        user.setRole(role);
        user.setStoreId(storeId);
        return user;
    }

    private void stubAuthenticatedUser(final User user) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(rolePermissionRepository.findPermissionCodesByRoleId(3L))
                .thenReturn(List.of("PURCHASE_ORDER_CREATE"));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh");
        when(scopeAccessService.buildUserProfile(user))
                .thenReturn(new ScopeAccessProfile(true, List.of(ScopeAssignment.admin()), Set.of(), Set.of()));
    }

    private LoginRequest loginRequest(final String email) {
        final LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password");
        return request;
    }

    @Test
    void loginResponseCarriesStoreIdForStoreBoundUser() {
        final User user = userWithStore(55L);
        stubAuthenticatedUser(user);

        final LoginResponse response = authService().authenticate(loginRequest(user.getEmail())).loginResponse();

        assertThat(response.getUser().storeId()).isEqualTo(55L);
    }

    @Test
    void loginResponseStoreIdIsNullForNonStoreUser() {
        final User user = userWithStore(null);
        stubAuthenticatedUser(user);

        final LoginResponse response = authService().authenticate(loginRequest(user.getEmail())).loginResponse();

        assertThat(response.getUser().storeId()).isNull();
    }
}
