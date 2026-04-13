package com.stockops.dto;

import com.stockops.entity.NotificationType;
import java.time.Instant;

/**
 * Notification response payload.
 *
 * @param id notification identifier
 * @param userId target user identifier
 * @param type notification category
 * @param title short notification title
 * @param message detailed notification message
 * @param read read state flag
 * @param createdAt notification creation timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record NotificationDTO(
        Long id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt) {
}
