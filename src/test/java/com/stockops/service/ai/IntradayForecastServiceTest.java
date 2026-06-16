package com.stockops.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.ai.forecast.ForecastModel;
import com.stockops.dto.ForecastProposalRunDTO;
import com.stockops.entity.Product;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.User;
import com.stockops.entity.ai.ForecastProposalRun;
import com.stockops.entity.ai.ForecastProposalStatus;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.ai.ForecastProposalRunRepository;
import com.stockops.security.ScopeGuard;
import com.stockops.service.PurchaseOrderService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Unit tests for the approve/reject lifecycle guards of {@link IntradayForecastService}.
 */
class IntradayForecastServiceTest {

    private ForecastProposalRunRepository proposalRepository;
    private PurchaseOrderService purchaseOrderService;
    private ScopeGuard scopeGuard;
    private ProductRepository productRepository;
    private IntradayForecastService service;
    private User user;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ForecastProposalRunRepository.class);
        purchaseOrderService = mock(PurchaseOrderService.class);
        scopeGuard = mock(ScopeGuard.class);
        productRepository = mock(ProductRepository.class);
        user = mock(User.class);
        when(user.getId()).thenReturn(7L);

        service = new IntradayForecastService(
                new IntradayForecastProperties(),
                new AIRecommendationProperties(),
                mock(NamedParameterJdbcTemplate.class),
                productRepository,
                proposalRepository,
                purchaseOrderService,
                scopeGuard,
                mock(ForecastModel.class),
                Map.of());

        when(proposalRepository.save(any(ForecastProposalRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        final Product product = mock(Product.class);
        when(product.getId()).thenReturn(3L);
        when(product.getName()).thenReturn("Test Product");
        when(product.getBarcode()).thenReturn("BC-3");
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
    }

    @Test
    void approveCreatesDraftPurchaseOrderAndMarksApproved() {
        final ForecastProposalRun proposal = openProposal(5);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        final PurchaseOrder po = mock(PurchaseOrder.class);
        when(po.getId()).thenReturn(100L);
        when(po.getPoNumber()).thenReturn("PO-1");
        when(purchaseOrderService.create(1L, 2L, user)).thenReturn(po);
        when(purchaseOrderService.addItem(100L, 3L, 5)).thenReturn(po);

        final ForecastProposalRunDTO dto = service.approveProposal(1L, user);

        assertThat(dto.status()).isEqualTo(ForecastProposalStatus.APPROVED_TO_DRAFT);
        assertThat(dto.approvedPurchaseOrderId()).isEqualTo(100L);
        assertThat(dto.approvedPurchaseOrderNumber()).isEqualTo("PO-1");
        assertThat(proposal.getApprovedBy()).isSameAs(user);
        verify(purchaseOrderService).addItem(100L, 3L, 5);
    }

    @Test
    void approveSupersedesSiblingOpenProposalsForSameScope() {
        final ForecastProposalRun approved = openProposal(5);
        approved.setId(1L);
        approved.setBusinessDate(LocalDate.of(2026, 5, 1));
        approved.setRunSlot(10);
        final ForecastProposalRun sibling = openProposal(7);
        sibling.setId(2L);
        sibling.setBusinessDate(LocalDate.of(2026, 5, 1));
        sibling.setRunSlot(15);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(approved));
        final PurchaseOrder po = mock(PurchaseOrder.class);
        when(po.getId()).thenReturn(100L);
        when(po.getPoNumber()).thenReturn("PO-1");
        when(purchaseOrderService.create(1L, 2L, user)).thenReturn(po);
        when(purchaseOrderService.addItem(100L, 3L, 5)).thenReturn(po);
        when(proposalRepository.findByBusinessDateAndProductIdAndCenterIdAndWarehouseIdAndStatus(
                any(), eq(3L), eq(1L), eq(2L), eq(ForecastProposalStatus.PROPOSED)))
                .thenReturn(List.of(approved, sibling));

        service.approveProposal(1L, user);

        assertThat(approved.getStatus()).isEqualTo(ForecastProposalStatus.APPROVED_TO_DRAFT);
        assertThat(sibling.getStatus()).isEqualTo(ForecastProposalStatus.EXPIRED);
    }

    @Test
    void approveRejectsProposalPastActionableWindow() {
        final ForecastProposalRun proposal = openProposal(5);
        proposal.setActionableUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> service.approveProposal(1L, user))
                .isInstanceOf(InvalidOperationException.class);
        verify(purchaseOrderService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    void approveRejectsAlreadyDecidedProposal() {
        final ForecastProposalRun proposal = openProposal(5);
        proposal.setStatus(ForecastProposalStatus.REJECTED);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> service.approveProposal(1L, user))
                .isInstanceOf(InvalidOperationException.class);
        verify(purchaseOrderService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    void approveRejectsNonPositiveQuantity() {
        final ForecastProposalRun proposal = openProposal(0);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> service.approveProposal(1L, user))
                .isInstanceOf(InvalidOperationException.class);
        verify(purchaseOrderService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    void rejectMarksProposalRejectedWithReason() {
        final ForecastProposalRun proposal = openProposal(5);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        final ForecastProposalRunDTO dto = service.rejectProposal(1L, user, "too much stock");

        assertThat(dto.status()).isEqualTo(ForecastProposalStatus.REJECTED);
        assertThat(dto.rejectionReason()).isEqualTo("too much stock");
        assertThat(dto.rejectedByUserId()).isEqualTo(7L);
        verify(purchaseOrderService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    void rejectRejectsProposalPastActionableWindow() {
        final ForecastProposalRun proposal = openProposal(5);
        proposal.setActionableUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> service.rejectProposal(1L, user, "late"))
                .isInstanceOf(InvalidOperationException.class);
    }

    private ForecastProposalRun openProposal(final int recommendedQuantity) {
        final ForecastProposalRun proposal = new ForecastProposalRun();
        proposal.setProductId(3L);
        proposal.setCenterId(1L);
        proposal.setWarehouseId(2L);
        proposal.setRecommendedQuantity(recommendedQuantity);
        proposal.setStatus(ForecastProposalStatus.PROPOSED);
        proposal.setActionableUntil(Instant.now().plus(1, ChronoUnit.DAYS));
        return proposal;
    }
}
