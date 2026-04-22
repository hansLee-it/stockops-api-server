package com.stockops.security;

import com.stockops.repository.UserRepository;
import com.stockops.repository.RolePermissionRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads application users from the database for Spring Security authentication.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ScopeAccessService scopeAccessService;

    /**
     * Creates the service.
     *
     * @param userRepository user repository
     * @param rolePermissionRepository role-permission repository
     */
    public CustomUserDetailsService(final UserRepository userRepository,
                                    final RolePermissionRepository rolePermissionRepository,
                                    final ScopeAccessService scopeAccessService) {
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.scopeAccessService = scopeAccessService;
    }

    /**
     * Loads a user by email for authentication.
     *
     * @param username user email address
     * @return Spring Security user details
     * @throws UsernameNotFoundException when the email does not exist
     */
    @Override
    public UserDetails loadUserByUsername(final String username) {
        final com.stockops.entity.User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        rolePermissionRepository.findPermissionCodesByRoleId(user.getRole().getId())
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        return new ScopedUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                authorities,
                scopeAccessService.buildUserProfile(user));
    }
}
