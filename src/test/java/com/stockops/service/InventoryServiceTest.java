package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockops.entity.Inventory;
import com.stockops.entity.Lot;
import com.stockops.exception.InsufficientStockException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.InventoryTransactionRepository;
import com.stockops.repository.LotRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private LotRepository lotRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void increaseStockCreatesNewInventoryWhenNotExists() {
        final Lot lot = new Lot();
        lot.setId(1L);
        lot.setProductId(10L);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(inventoryRepository.findForUpdate(10L, 20L, 1L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        final Inventory result = inventoryService.increaseStock(10L, 20L, 1L, 5, "INBOUND", 100L, 1L);

        assertThat(result.getQuantity()).isEqualTo(5);
    }

    @Test
    void decreaseStockThrowsWhenInsufficient() {
        final Lot lot = new Lot();
        lot.setId(1L);
        lot.setProductId(10L);

        final Inventory inventory = new Inventory();
        inventory.setId(100L);
        inventory.setProductId(10L);
        inventory.setLocationId(20L);
        inventory.setLotId(1L);
        inventory.setQuantity(3);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(inventoryRepository.findForUpdate(10L, 20L, 1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.decreaseStock(10L, 20L, 1L, 5, "OUTBOUND", 200L, 1L))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void increaseStockThrowsWhenQuantityNotPositive() {
        assertThatThrownBy(() -> inventoryService.increaseStock(10L, 20L, 1L, 0, "INBOUND", 100L, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Quantity must be greater than zero");
    }
}
