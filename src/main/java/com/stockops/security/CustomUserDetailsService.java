package com.stockops.security;

import com.stockops.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

    /**
     * Creates the service.
     *
     * @param userRepository user repository
     */
    public CustomUserDetailsService(final UserRepository userRepository) {
        this.userRepository = userRepository;
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

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())))
                .build();
    }
}
