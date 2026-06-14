package com.stockops.environment.command;

/**
 * JSON payload published to the controller command topic.
 *
 * @param correlationId unique id echoed back in the ACK
 * @param siteId Sensimul site identifier
 * @param controllerId Sensimul controller identifier
 * @param status requested status ("on"/"off")
 * @param outputLevel requested output level (0-100)
 * @author StockOps Team
 * @since 2.6
 */
public record ControllerCommandMessage(
        String correlationId,
        String siteId,
        String controllerId,
        String status,
        Integer outputLevel) {
}
