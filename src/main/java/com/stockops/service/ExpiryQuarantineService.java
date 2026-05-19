package com.stockops.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.entity.AuditLog;
import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.repository.AuditLogRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.LotRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the daily expiry quarantine batch.
 * Expired active lots are moved to quarantine together with their inventory balances,
 * and audit records are created for every automatic status transition.
 *
 * @author StockOps Team
 * @since 1.0
 * @see LotRepository
 * @see InventoryRepository
 * @see AuditLogRepository
 */
@Service
public class ExpiryQuarantineService {

    private static final String AUDIT_ACTION_AUTO_QUARANTINE = "AUTO_QUARANTINE";
    private static final String AUDIT_ENTITY_LOT = "Lot";
    private static final String AUDIT_ENTITY_INVENTORY = "Inventory";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final LotRepository lotRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Runs once per day at midnight in the business timezone.
     * Each lot is processed in its own transaction so one failure does not roll back the full batch.
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void autoQuarantineExpiredLots() {
        log.info("Starting expiry quarantine batch job");

        final LocalDate today = LocalDate.now(BUSINESS_ZONE);
        final List<Long> expiredLotIds = lotRepository.findExpiredLots(today).stream()
                .map(Lot::getId)
                .toList();

        log.info("Found {} expired lots to quarantine", expiredLotIds.size());

        int quarantinedCount = 0;
        for (final Long lotId : expiredLotIds) {
            try {
                final Boolean processed = transactionTemplate.execute(status -> quarantineLot(lotId, today));
                if (Boolean.TRUE.equals(processed)) {
                    quarantinedCount++;
                }
            } catch (final Exception exception) {
                log.error("Failed to quarantine lot id {}: {}", lotId, exception.getMessage(), exception);
            }
        }

        log.info("Expiry quarantine batch job completed. Quarantined {} of {} expired lots",
                quarantinedCount,
                expiredLotIds.size());
    }

    private boolean quarantineLot(final Long lotId, final LocalDate today) {
        final Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalStateException("Lot not found: " + lotId));

        if (lot.getExpiryDate() == null || !lot.getExpiryDate().isBefore(today) || lot.getStatus() != LotStatus.ACTIVE) {
            log.info("Skipping lot {} because it is no longer eligible for auto-quarantine", lot.getLotNumber());
            return false;
        }

        final LotStatus previousLotStatus = lot.getStatus();
        lot.setStatus(LotStatus.QUARANTINE);
        lotRepository.save(lot);

        final List<Inventory> inventories = inventoryRepository.findByLotId(lot.getId());
        for (final Inventory inventory : inventories) {
            quarantineInventory(inventory);
        }

        recordAuditLog(AUDIT_ENTITY_LOT, lot.getId(), previousLotStatus.name(), LotStatus.QUARANTINE.name());
        log.info("Quarantined lot {} (expiry: {})", lot.getLotNumber(), lot.getExpiryDate());
        return true;
    }

    private void quarantineInventory(final Inventory inventory) {
        final InventoryStatus previousInventoryStatus = inventory.getStatus();
        if (previousInventoryStatus == InventoryStatus.QUARANTINE) {
            return;
        }

        inventory.setStatus(InventoryStatus.QUARANTINE);
        inventoryRepository.save(inventory);
        recordAuditLog(
                AUDIT_ENTITY_INVENTORY,
                inventory.getId(),
                previousInventoryStatus == null ? null : previousInventoryStatus.name(),
                InventoryStatus.QUARANTINE.name());
    }

    private void recordAuditLog(final String entityType,
                                final Long entityId,
                                final String oldValue,
                                final String newValue) {
        final AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AUDIT_ACTION_AUTO_QUARANTINE);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setPerformedBy(null);
        auditLog.setPerformedAt(Instant.now());
        auditLogRepository.save(auditLog);
    }

    private static final Logger log = LoggerFactory.getLogger(ExpiryQuarantineService.class);

    public ExpiryQuarantineService(final LotRepository lotRepository, final InventoryRepository inventoryRepository, final AuditLogRepository auditLogRepository, final TransactionTemplate transactionTemplate) {
        this.lotRepository = lotRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionTemplate = transactionTemplate;
    }
}
