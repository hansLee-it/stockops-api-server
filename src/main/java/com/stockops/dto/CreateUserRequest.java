package com.stockops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

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
        String role,
        List<ScopeAssignmentRequest> scopeAssignments
) {
}
