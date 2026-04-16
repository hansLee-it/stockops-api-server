package com.stockops.integration.sensimul;

import com.stockops.exception.InvalidOperationException;

/**
 * Controller update payload for the Sensimul form route.
 *
 * @param status controller status, on or off
 * @param outputLevel controller output level from 0 to 100
 * @author StockOps Team
 * @since 1.0
 */
public record ControllerUpdateRequest(String status, Integer outputLevel) {

    /**
     * Validates the update payload.
     *
     * @throws InvalidOperationException when the request is invalid
     */
    public ControllerUpdateRequest {
        if (!"on".equals(status) && !"off".equals(status)) {
            throw new InvalidOperationException("status must be 'on' or 'off'");
        }
        if (outputLevel == null || outputLevel < 0 || outputLevel > 100) {
            throw new InvalidOperationException("output_level must be between 0 and 100");
        }
    }
}
