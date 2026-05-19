package com.stockops.notification.sms;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Audit log for outbound SMS dispatches.
 * Stores every send attempt (success or failure) for traceability and debugging.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(
        name = "sms_send_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sms_history_message_id",
                columnNames = {"message_id"}))
public class SmsSendHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Provider-assigned message identifier. null when send failed.
     */
    @Column(name = "message_id", length = 100)
    private String messageId;

    /**
     * Destination phone number in E.164 format.
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Text content that was sent.
     */
    @Column(name = "message", nullable = false, length = 1600)
    private String message;

    /**
     * Delivery outcome.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SmsSendStatus status;

    /**
     * Error description populated when status is FAILURE.
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Number of dispatch attempts made (1 for success on first try).
     */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /**
     * Instant when the message was successfully delivered (null if not yet sent).
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Send status enumeration.
     */
    public enum SmsSendStatus {
        /**
         * Accepted by provider and en route to carrier.
         */
        SENT,
        /**
         * Permanently rejected after all retry attempts.
         */
        FAILURE,
        /**
         * Accepted but delivery not yet confirmed (future-use for delivery receipts).
         */
        PENDING
    }

    public SmsSendHistory() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public SmsSendStatus getStatus() {
        return this.status;
    }

    public void setStatus(final SmsSendStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getAttemptCount() {
        return this.attemptCount;
    }

    public void setAttemptCount(final int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getSentAt() {
        return this.sentAt;
    }

    public void setSentAt(final Instant sentAt) {
        this.sentAt = sentAt;
    }
}