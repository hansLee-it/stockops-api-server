package com.stockops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * User creation request payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        String role
) {
}
