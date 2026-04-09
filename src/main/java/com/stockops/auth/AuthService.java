package com.stockops.auth;

import com.stockops.entity.User;
import com.stockops.repository.UserRepository;
import com.stockops.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication business logic for login and token renewal.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String BEARER_PREFIX = TOKEN_TYPE + " ";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Creates the authentication service.
     *
     * @param authenticationManager Spring Security authentication manager
     * @param userRepository user repository
     * @param jwtTokenProvider JWT utility component
     */
    public AuthService(final AuthenticationManager authenticationManager,
                       final UserRepository userRepository,
                       final JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticates a user and returns a signed access token.
     *
     * @param loginRequest login request payload
     * @return signed access token response
     * @throws ResponseStatusException when the credentials are invalid
     */
    public LoginResponse authenticate(final LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password", exception);
        }

        final User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return buildResponse(jwtTokenProvider.generateAccessToken(user));
    }

    /**
     * Refreshes an access token from the supplied bearer token.
     *
     * @param authorizationHeader Authorization header containing a bearer token
     * @return newly issued access token response
     * @throws ResponseStatusException when the header is missing or the token is invalid
     */
    public LoginResponse refreshToken(final String authorizationHeader) {
        final String token = resolveBearerToken(authorizationHeader);

        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        try {
            return buildResponse(jwtTokenProvider.refreshAccessToken(token));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token", exception);
        }
    }

    private LoginResponse buildResponse(final String accessToken) {
        return new LoginResponse(accessToken, TOKEN_TYPE, jwtTokenProvider.getExpiration());
    }

    private String resolveBearerToken(final String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
