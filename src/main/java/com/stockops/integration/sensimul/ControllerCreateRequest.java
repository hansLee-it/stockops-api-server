package com.stockops.integration.sensimul;

import com.stockops.exception.InvalidOperationException;

/**
 * Controller creation payload for the Sensimul form route.
 *
 * @param name controller name
 * @param type controller type
 * @author StockOps Team
 * @since 1.0
 */
public record ControllerCreateRequest(String name, String type) {

    /**
     * Validates the create payload.
     *
     * @throws InvalidOperationException when the request is invalid
     */
    public ControllerCreateRequest {
        if (name == null || name.isBlank()) {
            throw new InvalidOperationException("name is required");
        }
        if (type == null || type.isBlank()) {
            throw new InvalidOperationException("type is required");
        }
    }
}
