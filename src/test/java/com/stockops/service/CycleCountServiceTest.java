package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.dto.CompleteCycleCountItemRequest;
import com.stockops.dto.CompleteCycleCountRequest;
import com.stockops.dto.CreateCycleCountRequest;
import com.stockops.dto.CycleCountDTO;
import com.stockops.dto.CycleCountItemDTO;
import com.stockops.entity.CycleCount;
import com.stockops.entity.CycleCountItem;
import com.stockops.entity.CycleCountStatus;
import com.stockops.entity.Inventory;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.CycleCountItemRepository;
import com.stockops.repository.CycleCountRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CycleCountService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class CycleCountServiceTest {

    @Mock
    private CycleCountRepository cycleCountRepository;

    @Mock
    private CycleCountItemRepository cycleCountItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CycleCountService cycleCountService;

    /**
     * Verifies that a cycle count snapshots the requested inventory balances.
     */
    @Test
    void createCycleCountSnapshotsExpectedQuantities() {
        final LocalDate countDate = LocalDate.of(2026, 4, 9);
        final CreateCycleCountRequest request = new CreateCycleCountRequest(countDate, 300L, List.of(100L));

        final CycleCount savedCycleCount = new CycleCount();
        savedCycleCount.setId(10L);
        savedCycleCount.setCountDate(countDate);
        savedCycleCount.setStatus(CycleCountStatus.PENDING);
        savedCycleCount.setLocationId(300L);
        savedCycleCount.setCreatedBy(9L);

        final CycleCountItem item = new CycleCountItem();
        item.setId(1L);
        item.setCycleCountId(10L);
        item.setInventoryId(100L);
        item.setExpectedQuantity(7);

        final Inventory inventory = new Inventory();
        inventory.setId(100L);
        inventory.setLocationId(300L);
        inventory.setQuantity(7);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(locationRepository.existsById(300L)).thenReturn(true);
        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(cycleCountRepository.save(any(CycleCount.class))).thenReturn(savedCycleCount);
        when(cycleCountItemRepository.saveAll(any())).thenReturn(List.of(item));

        final CycleCountDTO dto = cycleCountService.createCycleCount(request, 9L);

        final ArgumentCaptor<CycleCount> cycleCountCaptor = ArgumentCaptor.forClass(CycleCount.class);
        verify(cycleCountRepository).save(cycleCountCaptor.capture());
        assertThat(cycleCountCaptor.getValue().getStatus()).isEqualTo(CycleCountStatus.PENDING);
        assertThat(cycleCountCaptor.getValue().getLocationId()).isEqualTo(300L);

        final ArgumentCaptor<Iterable<CycleCountItem>> itemCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(cycleCountItemRepository).saveAll(itemCaptor.capture());
        assertThat(itemCaptor.getValue())
                .singleElement()
                .satisfies(savedItem -> {
                    assertThat(savedItem.getCycleCountId()).isEqualTo(10L);
                    assertThat(savedItem.getInventoryId()).isEqualTo(100L);
                    assertThat(savedItem.getExpectedQuantity()).isEqualTo(7);
                });
        assertThat(dto.status()).isEqualTo(CycleCountStatus.PENDING);
        assertThat(dto.items()).singleElement().extracting(CycleCountItemDTO::expectedQuantity).isEqualTo(7);
    }

    /**
     * Verifies that duplicate inventory ids are rejected before persistence.
     */
    @Test
    void createCycleCountRejectsDuplicateInventoryIds() {
        final CreateCycleCountRequest request = new CreateCycleCountRequest(LocalDate.of(2026, 4, 9), 300L, List.of(100L, 100L));

        when(userRepository.existsById(9L)).thenReturn(true);
        when(locationRepository.existsById(300L)).thenReturn(true);

        assertThrows(InvalidOperationException.class,
                () -> cycleCountService.createCycleCount(request, 9L));
    }

    /**
     * Verifies that starting a pending cycle count updates its workflow state.
     */
    @Test
    void startCycleCountTransitionsPendingCountToInProgress() {
        final CycleCount cycleCount = new CycleCount();
        cycleCount.setId(10L);
        cycleCount.setStatus(CycleCountStatus.PENDING);

        final CycleCountItem item = new CycleCountItem();
        item.setId(1L);
        item.setCycleCountId(10L);
        item.setInventoryId(100L);
        item.setExpectedQuantity(5);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(cycleCountRepository.findById(10L)).thenReturn(Optional.of(cycleCount));
        when(cycleCountRepository.save(any(CycleCount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cycleCountItemRepository.findByCycleCountIdOrderByIdAsc(10L)).thenReturn(List.of(item));

        final CycleCountDTO dto = cycleCountService.startCycleCount(10L, 9L);

        assertThat(dto.status()).isEqualTo(CycleCountStatus.IN_PROGRESS);
        final ArgumentCaptor<CycleCount> cycleCountCaptor = ArgumentCaptor.forClass(CycleCount.class);
        verify(cycleCountRepository).save(cycleCountCaptor.capture());
        assertThat(cycleCountCaptor.getValue().getStatus()).isEqualTo(CycleCountStatus.IN_PROGRESS);
    }

    /**
     * Verifies that cycle counts are listed with their detail rows.
     */
    @Test
    void listCycleCountsReturnsCountsWithItems() {
        final CycleCount cycleCount = new CycleCount();
        cycleCount.setId(10L);
        cycleCount.setStatus(CycleCountStatus.PENDING);
        cycleCount.setLocationId(300L);
        cycleCount.setCreatedBy(9L);

        final CycleCountItem item = new CycleCountItem();
        item.setId(1L);
        item.setCycleCountId(10L);
        item.setInventoryId(100L);
        item.setExpectedQuantity(5);

        when(cycleCountRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(cycleCount));
        when(cycleCountItemRepository.findByCycleCountIdOrderByIdAsc(10L)).thenReturn(List.of(item));

        final List<CycleCountDTO> results = cycleCountService.listCycleCounts();

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.items()).singleElement()
                    .extracting(CycleCountItemDTO::inventoryId)
                    .isEqualTo(100L);
        });
    }

    /**
     * Verifies that completing a cycle count stores final quantities and variance metadata.
     */
    @Test
    void completeCycleCountStoresActualQuantitiesAndMarksCountCompleted() {
        final CycleCount cycleCount = new CycleCount();
        cycleCount.setId(10L);
        cycleCount.setStatus(CycleCountStatus.IN_PROGRESS);

        final CycleCountItem item = new CycleCountItem();
        item.setId(1L);
        item.setCycleCountId(10L);
        item.setInventoryId(100L);
        item.setExpectedQuantity(7);

        final CompleteCycleCountRequest request = new CompleteCycleCountRequest(
                List.of(new CompleteCycleCountItemRequest(1L, 10, "checked twice")));

        when(userRepository.existsById(9L)).thenReturn(true);
        when(cycleCountRepository.findById(10L)).thenReturn(Optional.of(cycleCount));
        when(cycleCountItemRepository.findByCycleCountIdOrderByIdAsc(10L)).thenReturn(List.of(item));
        when(cycleCountItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cycleCountRepository.save(any(CycleCount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final CycleCountDTO dto = cycleCountService.completeCycleCount(10L, request, 9L);

        assertThat(dto.status()).isEqualTo(CycleCountStatus.COMPLETED);
        assertThat(dto.completedBy()).isEqualTo(9L);
        assertThat(dto.items()).singleElement().satisfies(savedItem -> {
            assertThat(savedItem.actualQuantity()).isEqualTo(10);
            assertThat(savedItem.variance()).isEqualTo(3);
            assertThat(savedItem.countedBy()).isEqualTo(9L);
            assertThat(savedItem.notes()).isEqualTo("checked twice");
        });
        final ArgumentCaptor<CycleCount> cycleCountCaptor = ArgumentCaptor.forClass(CycleCount.class);
        verify(cycleCountRepository).save(cycleCountCaptor.capture());
        assertThat(cycleCountCaptor.getValue().getStatus()).isEqualTo(CycleCountStatus.COMPLETED);
        assertThat(cycleCountCaptor.getValue().getCompletedBy()).isEqualTo(9L);
        assertThat(cycleCountCaptor.getValue().getCompletedAt()).isNotNull();
    }
}
