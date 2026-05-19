package com.stockops.controller;

import com.stockops.dto.NotificationDTO;
import com.stockops.service.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification API controller.
 * Exposes authenticated inbox operations for listing notifications and
 * updating read state.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Returns notifications for the current user.
     *
     * @param principal authenticated principal
     * @param includeRead whether read notifications should be included
     * @return notification list ordered by newest first
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getNotifications(final Principal principal,
                                                                  @RequestParam(defaultValue = "true") final boolean includeRead) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(principal.getName(), includeRead));
    }

    /**
     * Returns unread badge count for the current user.
     *
     * @param principal authenticated principal
     * @return unread count payload
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(final Principal principal) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnreadNotifications(principal.getName())));
    }

    /**
     * Marks one notification as read.
     *
     * @param principal authenticated principal
     * @param id notification identifier
     * @return empty success response
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(final Principal principal, @PathVariable final Long id) {
        notificationService.markAsRead(principal.getName(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Marks all notifications as read for the current user.
     *
     * @param principal authenticated principal
     * @return empty success response
     */
    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(final Principal principal) {
        notificationService.markAllAsRead(principal.getName());
        return ResponseEntity.ok().build();
    }

    public NotificationController(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

}
