package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockops.entity.Inbound;
import com.stockops.entity.InboundItem;
import com.stockops.entity.Inventory;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.entity.Product;
import com.stockops.entity.Location;
import com.stockops.entity.Warehouse;
import com.stockops.entity.WarehouseStatus;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.InboundItemRepository;
import com.stockops.repository.InboundRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.config.MetricsConfig;
import com.stockops.inventory.WebSocketStockPublisher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboundServiceTest {

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private InboundItemRepository inboundItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WebSocketStockPublisher webSocketStockPublisher;

    @Mock
    private MetricsConfig metricsConfig;

    @InjectMocks
    private InboundService inboundService;

    @Test
    void confirmInboundThrowsWhenNoItems() {
        final Inbound inbound = new Inbound();
        inbound.setId(1L);
        inbound.setStatus("DRAFT");

        when(inboundRepository.findById(1L)).thenReturn(Optional.of(inbound));
        when(inboundItemRepository.findByInboundId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> inboundService.confirmInbound(1L, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Cannot confirm inbound with no items");
    }

    @Test
    void confirmInboundThrowsWhenNotFound() {
        when(inboundRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inboundService.confirmInbound(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Inbound not found: 99");
    }
}
