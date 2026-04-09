package com.stockops.exception;

/**
 * Standard API error payload.
 *
 * @author StockOps Team
 * @since 1.0
 */
public record ErrorResponse(int status, String message) {
}
