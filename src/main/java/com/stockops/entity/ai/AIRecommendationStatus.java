package com.stockops.entity.ai;

/**
 * Lifecycle state for persisted AI reorder recommendations.
 *
 * @author StockOps Team
 * @since 2.0
 */
public enum AIRecommendationStatus {
    READY_FOR_APPROVAL,
    NO_ACTION,
    INSUFFICIENT_HISTORY,
    APPROVED_TO_DRAFT
}
