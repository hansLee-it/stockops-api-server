package com.stockops.dto;

import com.stockops.exception.InvalidOperationException;

/**
 * Supported Excel import/export entity types.
 *
 * @author StockOps Team
 * @since 1.0
 */
public enum ExcelEntityType {
    PRODUCTS("products"),
    INBOUNDS("inbounds"),
    PURCHASE_ORDERS("purchase-orders");

    private final String pathValue;

    ExcelEntityType(final String pathValue) {
        this.pathValue = pathValue;
    }

    /**
     * Resolves the path value used by the HTTP API.
     *
     * @param value path segment value
     * @return matching entity type
     * @throws InvalidOperationException when the value is unsupported
     */
    public static ExcelEntityType fromPathValue(final String value) {
        for (ExcelEntityType entityType : values()) {
            if (entityType.pathValue.equalsIgnoreCase(value)) {
                return entityType;
            }
        }

        throw new InvalidOperationException("Unsupported Excel entity type: " + value);
    }

    /**
     * Returns the REST path value.
     *
     * @return lowercase path value
     */
    public String getPathValue() {
        return pathValue;
    }
}
