package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.entity.Outbound;
import com.stockops.entity.OutboundItem;
import com.stockops.exception.ForbiddenException;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.InventoryTransactionRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.OutboundItemRepository;
import com.stockops.repository.OutboundRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.config.MetricsConfig;
import com.stockops.security.CurrentUserProvider;
import com.stockops.security.ScopeGuard;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboundServiceTest {

    @Mock
    private OutboundRepository outboundRepository;

    @Mock
    private OutboundItemRepository outboundItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ScopeGuard scopeGuard;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MetricsConfig metricsConfig;

    @InjectMocks
    private OutboundService outboundService;

    @Test
    void confirmOutboundRejectsAllocationThatWouldCrossScopeBoundary() {
        final Outbound outbound = new Outbound();
        outbound.setId(1L);
        outbound.setOutboundDate(LocalDate.of(2026, 4, 21));
        outbound.setStatus("DRAFT");
        outbound.setCreatedBy(7L);

        final OutboundItem item = new OutboundItem();
        item.setId(11L);
        item.setOutboundId(1L);
        item.setProductId(101L);
        item.setQuantity(5);

        final Lot lot = new Lot();
        lot.setId(501L);
        lot.setStatus(LotStatus.ACTIVE);

        final Inventory inaccessibleInventory = new Inventory();
        inaccessibleInventory.setId(900L);
        inaccessibleInventory.setProductId(101L);
        inaccessibleInventory.setLotId(501L);
        inaccessibleInventory.setLocationId(44L);
        inaccessibleInventory.setQuantity(5);
        inaccessibleInventory.setStatus(InventoryStatus.ACTIVE);

        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);
        when(outboundRepository.findById(1L)).thenReturn(Optional.of(outbound));
        when(inventoryTransactionRepository.findByTypeAndReferenceIdOrderByCreatedAtDesc("OUTBOUND", 1L)).thenReturn(List.of());
        when(outboundItemRepository.findByOutboundId(1L)).thenReturn(List.of(item));
        when(lotRepository.findActiveLotsByProductIdOrderByExpiryDateAsc(101L, LotStatus.ACTIVE)).thenReturn(List.of(lot));
        when(inventoryRepository.findByProductIdAndLotId(101L, 501L)).thenReturn(List.of(inaccessibleInventory));
        when(scopeGuard.filterByLocationScope(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> outboundService.confirmOutbound(1L, 7L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied for outbound inventory allocation");
    }
}
