package com.stockops.service;

import com.stockops.entity.ExpiryAlert;
import com.stockops.entity.Inventory;
import com.stockops.entity.Lot;
import com.stockops.repository.ExpiryAlertRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.LotRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates daily expiry alerts for active lots with remaining inventory.
 * Alert levels are bucketed into D-30, D-14, D-7, and D-1 severity bands.
 *
 * @author StockOps Team
 * @since 1.0
 * @see ExpiryAlertRepository
 * @see LotRepository
 * @see InventoryRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiryAlertService {

    private static final int INFO_THRESHOLD_DAYS = 30;
    private static final int NOTICE_THRESHOLD_DAYS = 14;
    private static final int WARNING_THRESHOLD_DAYS = 7;
    private static final int CRITICAL_THRESHOLD_DAYS = 1;

    private final ExpiryAlertRepository expiryAlertRepository;
    private final LotRepository lotRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Recalculates near-expiry alerts once per day at 01:00.
     * Existing unacknowledged alerts are replaced so the alert list always reflects current stock.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateExpiryAlerts() {
        log.info("Starting expiry alert calculation");

        final List<ExpiryAlert> oldAlerts = expiryAlertRepository.findByAcknowledgedFalse();
        if (!oldAlerts.isEmpty()) {
            expiryAlertRepository.deleteAll(oldAlerts);
        }

        final LocalDate today = LocalDate.now();
        final List<Lot> lotsWithExpiry = lotRepository.findActiveLotsWithExpiry();

        for (final Lot lot : lotsWithExpiry) {
            if (lot.getExpiryDate() == null) {
                continue;
            }

            final long daysUntilExpiry = ChronoUnit.DAYS.between(today, lot.getExpiryDate());
            if (daysUntilExpiry >= 0 && daysUntilExpiry <= INFO_THRESHOLD_DAYS) {
                createAlertIfNeeded(lot, (int) daysUntilExpiry);
            }
        }

        log.info("Expiry alert calculation completed");
    }

    private void createAlertIfNeeded(final Lot lot, final int daysUntilExpiry) {
        final int totalQuantity = inventoryRepository.findByLotId(lot.getId()).stream()
                .map(Inventory::getQuantity)
                .filter(quantity -> quantity != null && quantity > 0)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalQuantity <= 0) {
            return;
        }

        final ExpiryAlert alert = new ExpiryAlert();
        alert.setLotId(lot.getId());
        alert.setProductId(lot.getProductId());
        alert.setDaysUntilExpiry(daysUntilExpiry);
        alert.setAlertLevel(determineAlertLevel(daysUntilExpiry));
        alert.setExpiryDate(lot.getExpiryDate());
        alert.setQuantity(totalQuantity);
        alert.setAcknowledged(false);

        expiryAlertRepository.save(alert);

        log.info("Created {} alert for lot {} ({} days until expiry)",
                alert.getAlertLevel(),
                lot.getLotNumber(),
                daysUntilExpiry);
    }

    private String determineAlertLevel(final int daysUntilExpiry) {
        if (daysUntilExpiry <= CRITICAL_THRESHOLD_DAYS) {
            return "CRITICAL";
        }
        if (daysUntilExpiry <= WARNING_THRESHOLD_DAYS) {
            return "WARNING";
        }
        if (daysUntilExpiry <= NOTICE_THRESHOLD_DAYS) {
            return "NOTICE";
        }
        return "INFO";
    }
}
