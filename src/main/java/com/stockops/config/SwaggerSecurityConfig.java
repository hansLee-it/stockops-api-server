package com.stockops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Swagger / OpenAPI access control.
 * <p>
 * Allows public access to {@code /swagger-ui/**} and {@code /v3/api-docs/**}
 * only when the application is NOT running with the {@code prod} profile.
 * In production these endpoints remain blocked by the default {@link SecurityConfig}
 * which requires authentication for any unmatched path.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@Profile("!prod")
public class SwaggerSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain swaggerFilterChain(final HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
