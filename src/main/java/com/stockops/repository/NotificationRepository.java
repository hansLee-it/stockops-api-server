package com.stockops.repository;

import com.stockops.entity.Notification;
import com.stockops.entity.NotificationType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for user notification persistence.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Returns notifications for a user ordered by recency.
     *
     * @param userId target user id
     * @return user notifications ordered by creation time descending
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Returns unread notifications for a user ordered by recency.
     *
     * @param userId target user id
     * @return unread notifications ordered by creation time descending
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Counts unread notifications for a user.
     *
     * @param userId target user id
     * @return unread notification count
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Finds a notification by identifier scoped to a user.
     *
     * @param id notification id
     * @param userId target user id
     * @return matching notification when present
     */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /**
     * Finds a notification by its deduplication event key.
     *
     * @param userId target user id
     * @param eventKey unique event key
     * @return matching notification when present
     */
    Optional<Notification> findByUserIdAndEventKey(Long userId, String eventKey);

    /**
     * Checks if a notification exists for a user event.
     *
     * @param userId target user id
     * @param eventKey unique event key
     * @return {@code true} when a matching notification exists
     */
    boolean existsByUserIdAndEventKey(Long userId, String eventKey);

    /**
     * Returns notifications of the supplied types for a user.
     *
     * @param userId target user id
     * @param types notification types to include
     * @return matching notifications
     */
    List<Notification> findByUserIdAndTypeIn(Long userId, Collection<NotificationType> types);
}
