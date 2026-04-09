package com.stockops.repository;

import com.stockops.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for application users.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email address.
     *
     * @param email unique user email
     * @return matching user when present
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user exists for the given email address.
     *
     * @param email unique user email
     * @return {@code true} when the email is already registered
     */
    boolean existsByEmail(String email);
}
