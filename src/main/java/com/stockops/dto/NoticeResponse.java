package com.stockops.dto;

import com.stockops.entity.Notice;
import com.stockops.entity.NoticeType;
import java.time.Instant;

public record NoticeResponse(
        Long id,
        String title,
        String content,
        NoticeType type,
        boolean active,
        Long createdBy,
        Instant noticeAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoticeResponse from(final Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getType(),
                Boolean.TRUE.equals(notice.getActive()),
                notice.getCreatedBy(),
                notice.getNoticeAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
