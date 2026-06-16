package com.stockops.entity.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ForecastProposalRun#isActionable} — the 3-day approve/reject window guard.
 */
class ForecastProposalRunTest {

    private static final Instant NOW = Instant.parse("2026-06-16T05:00:00Z");

    @Test
    void openProposalWithinWindowIsActionable() {
        final ForecastProposalRun proposal = proposal(ForecastProposalStatus.PROPOSED, NOW.plus(1, ChronoUnit.DAYS));
        assertThat(proposal.isActionable(NOW)).isTrue();
    }

    @Test
    void openProposalAtExactDeadlineIsActionable() {
        final ForecastProposalRun proposal = proposal(ForecastProposalStatus.PROPOSED, NOW);
        assertThat(proposal.isActionable(NOW)).isTrue();
    }

    @Test
    void openProposalPastWindowIsNotActionable() {
        final ForecastProposalRun proposal = proposal(ForecastProposalStatus.PROPOSED, NOW.minus(1, ChronoUnit.SECONDS));
        assertThat(proposal.isActionable(NOW)).isFalse();
    }

    @Test
    void approvedProposalIsNeverActionable() {
        final ForecastProposalRun proposal = proposal(ForecastProposalStatus.APPROVED_TO_DRAFT, NOW.plus(1, ChronoUnit.DAYS));
        assertThat(proposal.isActionable(NOW)).isFalse();
    }

    @Test
    void rejectedProposalIsNeverActionable() {
        final ForecastProposalRun proposal = proposal(ForecastProposalStatus.REJECTED, NOW.plus(1, ChronoUnit.DAYS));
        assertThat(proposal.isActionable(NOW)).isFalse();
    }

    private ForecastProposalRun proposal(final ForecastProposalStatus status, final Instant actionableUntil) {
        final ForecastProposalRun proposal = new ForecastProposalRun();
        proposal.setStatus(status);
        proposal.setActionableUntil(actionableUntil);
        return proposal;
    }
}
