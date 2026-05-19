package com.stockops.auth;

import com.stockops.dto.ScopeMetadataDTO;

import java.util.List;

/**
 * Login and refresh response payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final Long expiresIn;
    private final AuthenticatedUser user;

    /**
     * Authenticated user profile returned with login responses.
     *
     * @param id user id
     * @param email user email
     * @param name user display name
     * @param role role name
     * @param permissions granted permission codes
     * @param scopeMetadata effective visibility metadata for scoped filtering
     */
    public record AuthenticatedUser(
            Long id,
            String email,
            String name,
            String role,
            List<String> permissions,
            ScopeMetadataDTO scopeMetadata
    ) {
    }

    public LoginResponse(final String accessToken, final String tokenType, final Long expiresIn, final AuthenticatedUser user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public Long getExpiresIn() {
        return this.expiresIn;
    }

    public AuthenticatedUser getUser() {
        return this.user;
    }
}
