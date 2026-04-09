package com.stockops.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
}
