package com.stockops.security;

import com.stockops.entity.User;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated StockOps user from Spring Security context.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    /**
     * Returns the authenticated user id.
     *
     * @return persisted user id
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Returns the authenticated user email, or {@code null} when unauthenticated.
     *
     * @return current user email
     */
    public String getCurrentUserEmail() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Returns the authenticated user entity.
     *
     * @return authenticated user
     */
    public User getCurrentUser() {
        final String email = getCurrentUserEmail();
        if (email == null) {
            throw new InvalidOperationException("Authenticated user is required");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOperationException("Authenticated user not found"));
    }

    public CurrentUserProvider(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
