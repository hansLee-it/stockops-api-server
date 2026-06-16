package com.stockops.entity.ai;

/**
 * Lifecycle status of an intraday forecast proposal.
 *
 * <p>A proposal is created as {@link #PROPOSED} and stays actionable until its
 * {@code actionableUntil} instant. From there it moves to {@link #APPROVED_TO_DRAFT}
 * (a draft purchase order was created) or {@link #REJECTED}. Proposals that age out of
 * the actionable window without a decision are {@link #EXPIRED} and remain as history only.
 *
 * @author StockOps Team
 * @since 2.4
 */
public enum ForecastProposalStatus {

    /** Open proposal awaiting an approve/reject decision. */
    PROPOSED,

    /** Approved by a user into a draft purchase order. */
    APPROVED_TO_DRAFT,

    /** Rejected by a user. */
    REJECTED,

    /** Aged past the actionable window without a decision; retained for history only. */
    EXPIRED
}
