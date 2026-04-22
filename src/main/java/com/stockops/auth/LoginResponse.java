package com.stockops.auth;

import com.stockops.dto.ScopeMetadataDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Login and refresh response payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
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
}
