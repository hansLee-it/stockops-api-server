package com.stockops.environment.command;

/**
 * JSON payload Sensimul publishes to the ACK topic after applying (or rejecting) a command.
 *
 * @param correlationId correlation id from the original command
 * @param resultStatus "APPLIED" or "FAILED"
 * @param resultCode short machine code (e.g. "OK", "INVALID")
 * @param message human-readable detail (optional)
 * @author StockOps Team
 * @since 2.6
 */
public record ControllerCommandAck(
        String correlationId,
        String resultStatus,
        String resultCode,
        String message) {
}
