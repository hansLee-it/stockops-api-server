package com.stockops.dto;

import com.stockops.entity.NoticeType;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record NoticeRequest(
        @NotBlank String title,
        String content,
        NoticeType type,
        Boolean active,
        Long createdBy,
        Instant noticeAt,
        List<String> targetRoles
) {
}
