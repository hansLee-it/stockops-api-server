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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit log for outbound SMS dispatches.
 * Stores every send attempt (success or failure) for traceability and debugging.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(
        name = "sms_send_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sms_history_message_id",
                columnNames = {"message_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
}