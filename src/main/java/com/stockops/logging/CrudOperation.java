package com.stockops.logging;

/**
 * CRUD operation type derived from HTTP method.
 *
 * <p>Determines the default log level when the HTTP response status is 2xx/3xx:
 * <ul>
 *   <li>{@link #READ}   — DEBUG (high-frequency; hidden at INFO in non-dev environments)</li>
 *   <li>{@link #CREATE} — INFO</li>
 *   <li>{@link #UPDATE} — INFO</li>
 *   <li>{@link #DELETE} — WARN (destructive; always visible)</li>
 *   <li>{@link #OTHER}  — INFO</li>
 * </ul>
 *
 * <p>4xx responses override to WARN; 5xx responses override to ERROR regardless of operation type.
 *
 * @author StockOps Team
 * @since 1.0
 * @see CrudRequestLoggingFilter
 */
public enum CrudOperation {

    /** HTTP GET / HEAD — data read. */
    READ("READ"),

    /** HTTP POST — resource creation. */
    CREATE("CREATE"),

    /** HTTP PUT / PATCH — resource update. */
    UPDATE("UPDATE"),

    /** HTTP DELETE — resource removal. */
    DELETE("DELETE"),

    /** Any other HTTP method. */
    OTHER("OTHER");

    private final String label;

    CrudOperation(final String label) {
        this.label = label;
    }

    /**
     * Returns the display label used in log messages.
     *
     * @return operation label string
     */
    public String label() {
        return label;
    }

    /**
     * Maps an HTTP method to its corresponding CRUD operation.
     *
     * @param httpMethod HTTP method (GET, POST, PUT, PATCH, DELETE, etc.)
     * @return corresponding CRUD operation; never null
     */
    public static CrudOperation from(final String httpMethod) {
        return switch (httpMethod.toUpperCase()) {
            case "GET", "HEAD"   -> READ;
            case "POST"          -> CREATE;
            case "PUT", "PATCH"  -> UPDATE;
            case "DELETE"        -> DELETE;
            default              -> OTHER;
        };
    }
}
