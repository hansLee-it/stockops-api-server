package com.stockops.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API controller.
 * Provides login, token refresh, and stateless logout endpoints.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Creates the authentication controller.
     *
     * @param authService authentication service
     */
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns a JWT access token.
     *
     * @param loginRequest login request payload
     * @return JWT access token response
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticate(loginRequest));
    }

    /**
     * Issues a new JWT access token from the current bearer token.
     *
     * @param authorizationHeader current bearer token header
     * @return refreshed JWT access token response
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) final String authorizationHeader) {
        return ResponseEntity.ok(authService.refreshToken(authorizationHeader));
    }

    /**
     * Completes stateless logout.
     * Clients should discard the bearer token on receipt of this response.
     *
     * @return empty successful response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
