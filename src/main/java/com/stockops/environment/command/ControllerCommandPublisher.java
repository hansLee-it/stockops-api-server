package com.stockops.environment.command;

/**
 * Publishes controller commands to the messaging broker. Implementations are responsible for
 * serialization and transport; the service layer stays transport-agnostic.
 *
 * @author StockOps Team
 * @since 2.6
 */
public interface ControllerCommandPublisher {

    /**
     * Publishes a command to the controller's command topic.
     *
     * @param message command payload (carries the correlation id)
     * @throws RuntimeException when publishing fails (the caller marks the command FAILED)
     */
    void publish(ControllerCommandMessage message);
}
