package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.entity.AuditLog;
import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.repository.AuditLogRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.LotRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Unit tests for {@link ExpiryQuarantineService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ExpiryQuarantineServiceTest {

    @Mock
    private LotRepository lotRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ExpiryQuarantineService expiryQuarantineService;

    /**
     * Verifies that expired active lots and their inventory rows are quarantined and audited.
     */
    @Test
    void autoQuarantineExpiredLotsQuarantinesExpiredInventoryAndCreatesAuditLogs() {
        final LocalDate expiredDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        final Lot lot = new Lot();
        lot.setId(1L);
        lot.setLotNumber("LOT-001");
        lot.setExpiryDate(expiredDate);
        lot.setStatus(LotStatus.ACTIVE);

        final Inventory activeInventory = new Inventory();
        activeInventory.setId(10L);
        activeInventory.setLotId(1L);
        activeInventory.setStatus(InventoryStatus.ACTIVE);

        final Inventory reservedInventory = new Inventory();
        reservedInventory.setId(11L);
        reservedInventory.setLotId(1L);
        reservedInventory.setStatus(InventoryStatus.RESERVED);

        when(lotRepository.findExpiredLots(any(LocalDate.class))).thenReturn(List.of(lot));
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(inventoryRepository.findByLotId(1L)).thenReturn(List.of(activeInventory, reservedInventory));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            final TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });

        expiryQuarantineService.autoQuarantineExpiredLots();

        verify(lotRepository).save(lot);
        verify(inventoryRepository, times(2)).save(any(Inventory.class));

        final ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(3)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getAllValues())
                .extracting(AuditLog::getEntityType, AuditLog::getEntityId, AuditLog::getAction, AuditLog::getNewValue)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("Inventory", 10L, "AUTO_QUARANTINE", "QUARANTINE"),
                        Tuple.tuple("Inventory", 11L, "AUTO_QUARANTINE", "QUARANTINE"),
                        Tuple.tuple("Lot", 1L, "AUTO_QUARANTINE", "QUARANTINE"));
        assertThat(lot.getStatus()).isEqualTo(LotStatus.QUARANTINE);
        assertThat(activeInventory.getStatus()).isEqualTo(InventoryStatus.QUARANTINE);
        assertThat(reservedInventory.getStatus()).isEqualTo(InventoryStatus.QUARANTINE);
    }

    /**
     * Verifies that already quarantined inventory rows are not rewritten or audited again.
     */
    @Test
    void autoQuarantineExpiredLotsSkipsInventoryAlreadyInQuarantine() {
        final LocalDate expiredDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        final Lot lot = new Lot();
        lot.setId(2L);
        lot.setLotNumber("LOT-002");
        lot.setExpiryDate(expiredDate);
        lot.setStatus(LotStatus.ACTIVE);

        final Inventory quarantinedInventory = new Inventory();
        quarantinedInventory.setId(20L);
        quarantinedInventory.setLotId(2L);
        quarantinedInventory.setStatus(InventoryStatus.QUARANTINE);

        when(lotRepository.findExpiredLots(any(LocalDate.class))).thenReturn(List.of(lot));
        when(lotRepository.findById(2L)).thenReturn(Optional.of(lot));
        when(inventoryRepository.findByLotId(2L)).thenReturn(List.of(quarantinedInventory));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            final TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });

        expiryQuarantineService.autoQuarantineExpiredLots();

        verify(inventoryRepository, never()).save(quarantinedInventory);
        final ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getEntityType()).isEqualTo("Lot");
    }
}
