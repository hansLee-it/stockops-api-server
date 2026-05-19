package com.stockops.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
