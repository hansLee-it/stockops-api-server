package com.stockops.exception;

/**
 * Raised when an outbound operation requests more stock than available.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int requested;
    private final int available;

    /**
     * Creates the exception with stock details.
     *
     * @param productId product id
     * @param requested requested quantity
     * @param available available quantity
     */
    public InsufficientStockException(final Long productId, final int requested, final int available) {
        super(String.format(
                "Insufficient stock for product %d: requested %d, available %d",
                productId,
                requested,
                available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    /**
     * Returns the product id.
     *
     * @return product id
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * Returns the requested quantity.
     *
     * @return requested quantity
     */
    public int getRequested() {
        return requested;
    }

    /**
     * Returns the available quantity.
     *
     * @return available quantity
     */
    public int getAvailable() {
        return available;
    }
}
