package com.stockops.auth;

import com.stockops.entity.Role;
import com.stockops.entity.User;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the default administrator account required for first-time access.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
public class AuthDataLoader implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@stockops.com";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the authentication data loader.
     *
     * @param userRepository user repository
     * @param roleRepository role repository
     * @param passwordEncoder password encoder
     */
    public AuthDataLoader(final UserRepository userRepository,
                          final RoleRepository roleRepository,
                          final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inserts the default administrator if it does not already exist.
     *
     * @param args application arguments
     */
    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        final Role adminRole = roleRepository.findByName(ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException("Missing role: " + ADMIN_ROLE));

        final User adminUser = new User();
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setName("StockOps Admin");
        adminUser.setEnabled(true);
        adminUser.setRole(adminRole);

        userRepository.save(adminUser);
    }
}
