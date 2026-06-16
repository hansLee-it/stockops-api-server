package com.stockops.controller;

import com.stockops.dto.ForecastProposalRunDTO;
import com.stockops.entity.User;
import com.stockops.service.UserService;
import com.stockops.service.ai.IntradayForecastService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for intraday forecast proposals.
 *
 * <p>Reuses the daily recommendation permissions: {@code AI_RECOMMENDATION_READ} to view proposals,
 * {@code AI_RECOMMENDATION_APPROVE} to approve/reject. Approve and reject are allowed only while a
 * proposal is within its actionable window (see {@code stockops.ai.intraday.actionable-days}).
 *
 * @author StockOps Team
 * @since 2.4
 */
@RestController
@RequestMapping("/api/v1/ai/intraday-proposals")
public class IntradayForecastController {

    private final IntradayForecastService intradayForecastService;
    private final UserService userService;

    public IntradayForecastController(final IntradayForecastService intradayForecastService,
            final UserService userService) {
        this.intradayForecastService = intradayForecastService;
        this.userService = userService;
    }

    /**
     * Lists scoped intraday proposals for a business date (defaults to today).
     *
     * @param businessDate optional business date filter
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @param productId optional product filter
     * @return scoped proposal payloads
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('AI_RECOMMENDATION_READ')")
    public List<ForecastProposalRunDTO> getProposals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate businessDate,
            @RequestParam(required = false) final Long centerId,
            @RequestParam(required = false) final Long warehouseId,
            @RequestParam(required = false) final Long productId) {
        return intradayForecastService.listProposals(businessDate, centerId, warehouseId, productId);
    }

    /**
     * Approves an open, in-window proposal into a draft purchase order.
     *
     * @param proposalId proposal identifier
     * @param principal authenticated principal
     * @return approved proposal payload with linked draft purchase-order data
     */
    @PostMapping("/{proposalId}/approve")
    @PreAuthorize("@permissionChecker.hasPermission('AI_RECOMMENDATION_APPROVE')")
    public ForecastProposalRunDTO approveProposal(@PathVariable final Long proposalId, final Principal principal) {
        final User currentUser = userService.getUserByEmail(principal.getName());
        return intradayForecastService.approveProposal(proposalId, currentUser);
    }

    /**
     * Rejects an open, in-window proposal.
     *
     * @param proposalId proposal identifier
     * @param reason optional rejection reason
     * @param principal authenticated principal
     * @return rejected proposal payload
     */
    @PostMapping("/{proposalId}/reject")
    @PreAuthorize("@permissionChecker.hasPermission('AI_RECOMMENDATION_APPROVE')")
    public ForecastProposalRunDTO rejectProposal(@PathVariable final Long proposalId,
            @RequestParam(required = false) final String reason,
            final Principal principal) {
        final User currentUser = userService.getUserByEmail(principal.getName());
        return intradayForecastService.rejectProposal(proposalId, currentUser, reason);
    }
}
