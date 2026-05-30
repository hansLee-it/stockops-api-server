package com.stockops.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AISuggestionRejectRequest(
        @NotBlank String rejectionReason
) {
}
