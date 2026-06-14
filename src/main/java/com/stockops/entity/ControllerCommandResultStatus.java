package com.stockops.entity;

/**
 * Processing result status for controller commands.
 *
 * @author StockOps Team
 * @since 1.0
 */
public enum ControllerCommandResultStatus {
    PENDING,
    FORWARDED,
    APPLIED,
    FAILED_RETRYABLE,
    // Messaging flow (stockops.command-messaging): PENDING -> SENT -> ACKED | FAILED | TIMEOUT
    SENT,
    ACKED,
    FAILED,
    TIMEOUT
}
