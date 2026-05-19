package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * In-app notification entity for user-specific operational alerts.
 * Stores the user target, message payload, and read state while reusing
 * the base audit fields for creation timestamps.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notifications_user_event_key",
                columnNames = {"user_id", "event_key"}))
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    public Notification() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return this.type;
    }

    public void setType(final NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public boolean isRead() {
        return this.read;
    }

    public void setRead(final boolean read) {
        this.read = read;
    }

    public String getEventKey() {
        return this.eventKey;
    }

    public void setEventKey(final String eventKey) {
        this.eventKey = eventKey;
    }
}
