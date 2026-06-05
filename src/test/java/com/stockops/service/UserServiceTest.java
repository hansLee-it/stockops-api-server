package com.stockops.service;

import com.stockops.dto.UpdateUserRequest;
import com.stockops.entity.Role;
import com.stockops.entity.User;
import com.stockops.exception.ForbiddenException;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.UserRepository;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ScopeAccessService scopeAccessService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder, scopeAccessService);
    }

    @Test
    void updateUserRejectsCurrentAdminRoleDemotion() {
        User currentAdmin = user(1L, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentAdmin));

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest(null, "MANAGER", null), 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Current administrator cannot remove their own ADMIN role");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserAllowsCurrentAdminNameChangeWhenRoleRemainsAdmin() {
        User currentAdmin = user(1L, "ADMIN");
        Role adminRole = role("ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentAdmin));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(currentAdmin)).thenReturn(currentAdmin);
        when(scopeAccessService.buildUserProfile(currentAdmin)).thenReturn(new ScopeAccessProfile(true, List.of(), Set.of(), Set.of()));

        userService.updateUser(1L, new UpdateUserRequest("Renamed Admin", "ADMIN", null), 1L);

        verify(userRepository).save(currentAdmin);
    }

    @Test
    void deleteUserRejectsCurrentAdminDeletion() {
        User currentAdmin = user(1L, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentAdmin));

        assertThatThrownBy(() -> userService.deleteUser(1L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Current administrator cannot delete their own ADMIN account");

        verify(userRepository, never()).deleteById(1L);
    }

    @Test
    void deleteUserAllowsDeletingDifferentAdminAccount() {
        User otherAdmin = user(2L, "ADMIN");
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));

        userService.deleteUser(2L, 1L);

        verify(userRepository).deleteById(2L);
    }

    private User user(final Long id, final String roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@stockops.test");
        user.setName("User " + id);
        user.setRole(role(roleName));
        return user;
    }

    private Role role(final String name) {
        Role role = new Role();
        role.setId(1L);
        role.setName(name);
        return role;
    }
}
