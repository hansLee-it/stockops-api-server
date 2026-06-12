package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Environment alert notification outbox row.
 *
 * <p>Telemetry ingestion inserts a {@code PENDING} row in the same transaction as the alert
 * change; the scheduled sender claims rows with {@code FOR UPDATE SKIP LOCKED} and delivers
 * webhook/email. This decouples delivery from ingestion and makes notifications retryable and
 * duplicate-safe across load-balanced instances.
 *
 * @author StockOps Team
 * @since 2.3
 */
@Entity
@Table(name = "environment_alert_notifications")
public class EnvironmentAlertNotification extends BaseEntity {

    /** What alert transition triggered the notification. */
    public enum TriggerType { OPENED, ESCALATED }

    /** Delivery lifecycle of an outbox row. */
    public enum Status { PENDING, SENT, FAILED, SUPERSEDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    public EnvironmentAlertNotification() {
    }

    /**
     * Creates a pending outbox row.
     *
     * @param alertId related environment alert id
     * @param triggerType alert transition that triggered the notification
     * @param severity severity at trigger time
     * @return pending notification row
     */
    public static EnvironmentAlertNotification pending(final Long alertId, final TriggerType triggerType,
                                                       final AlertSeverity severity) {
        final EnvironmentAlertNotification notification = new EnvironmentAlertNotification();
        notification.alertId = alertId;
        notification.triggerType = triggerType;
        notification.severity = severity;
        notification.status = Status.PENDING;
        return notification;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(final Long alertId) {
        this.alertId = alertId;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(final TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(final AlertSeverity severity) {
        this.severity = severity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(final int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(final Instant sentAt) {
        this.sentAt = sentAt;
    }
}
