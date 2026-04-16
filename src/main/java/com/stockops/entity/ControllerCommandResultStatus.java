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
    FAILED_RETRYABLE
}
