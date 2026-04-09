package com.stockops.service;

/**
 * Represents a single FEFO lot allocation generated while confirming an outbound.
 *
 * @param lotId allocated lot id
 * @param quantity quantity consumed from the lot
 * @author StockOps Team
 * @since 1.0
 */
public record OutboundLotAllocation(Long lotId, int quantity) {
}
