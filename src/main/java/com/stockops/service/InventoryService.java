package com.stockops.service;

import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.InventoryTransaction;
import com.stockops.entity.Lot;
import com.stockops.exception.InsufficientStockException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.InventoryTransactionRepository;
import com.stockops.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventory balance mutation service.
 * Applies pessimistic locking so concurrent inbound and outbound operations update stock atomically.
 *
 * @author StockOps Team
 * @since 1.0
 * @see InventoryRepository
 * @see InventoryTransactionRepository
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final LotRepository lotRepository;

    /**
     * Increases inventory for a product-location-lot combination.
     *
     * @param productId product id
     * @param locationId location id
     * @param lotId lot id
     * @param quantity quantity to add
     * @param referenceType source reference type
     * @param referenceId source reference id
     * @param userId operator id
     * @return updated inventory row
     * @throws ResourceNotFoundException when the lot does not exist
     * @throws InvalidOperationException when the quantity is not positive or lot ownership is invalid
     */
    @Transactional
    public Inventory increaseStock(final Long productId,
                                   final Long locationId,
                                   final Long lotId,
                                   final int quantity,
                                   final String referenceType,
                                   final Long referenceId,
                                   final Long userId) {
        validateRequiredIdentifiers(productId, locationId, lotId);
        validateReferenceType(referenceType);
        validateQuantity(quantity);
        validateLotOwnership(productId, lotId);

        final Inventory inventory = inventoryRepository.findForUpdate(productId, locationId, lotId)
                .orElseGet(() -> createNewInventory(productId, locationId, lotId));

        final int beforeQuantity = nullSafeQuantity(inventory.getQuantity());
        inventory.setQuantity(beforeQuantity + quantity);
        inventory.setStatus(InventoryStatus.ACTIVE);

        final Inventory saved = inventoryRepository.save(inventory);
        recordTransaction(productId, locationId, lotId, "INBOUND", quantity, beforeQuantity, saved.getQuantity(),
                referenceId, userId);
        return saved;
    }

    /**
     * Decreases inventory for a product-location-lot combination.
     *
     * @param productId product id
     * @param locationId location id
     * @param lotId lot id
     * @param quantity quantity to subtract
     * @param referenceType source reference type
     * @param referenceId source reference id
     * @param userId operator id
     * @return updated inventory row
     * @throws ResourceNotFoundException when inventory or lot does not exist
     * @throws InvalidOperationException when the quantity is not positive or lot ownership is invalid
     * @throws InsufficientStockException when available stock is lower than requested
     */
    @Transactional
    public Inventory decreaseStock(final Long productId,
                                   final Long locationId,
                                   final Long lotId,
                                   final int quantity,
                                   final String referenceType,
                                   final Long referenceId,
                                   final Long userId) {
        validateRequiredIdentifiers(productId, locationId, lotId);
        validateReferenceType(referenceType);
        validateQuantity(quantity);
        validateLotOwnership(productId, lotId);

        final Inventory inventory = inventoryRepository.findForUpdate(productId, locationId, lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        final int beforeQuantity = nullSafeQuantity(inventory.getQuantity());
        if (beforeQuantity < quantity) {
            throw new InsufficientStockException(productId, quantity, beforeQuantity);
        }

        inventory.setQuantity(beforeQuantity - quantity);
        final Inventory saved = inventoryRepository.save(inventory);
        recordTransaction(productId, locationId, lotId, "OUTBOUND", quantity, beforeQuantity, saved.getQuantity(),
                referenceId, userId);
        return saved;
    }

    private Inventory createNewInventory(final Long productId, final Long locationId, final Long lotId) {
        final Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setLocationId(locationId);
        inventory.setLotId(lotId);
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setStatus(InventoryStatus.ACTIVE);
        return inventoryRepository.save(inventory);
    }

    private void recordTransaction(final Long productId,
                                   final Long locationId,
                                   final Long lotId,
                                   final String type,
                                   final int quantity,
                                   final int before,
                                   final int after,
                                   final Long referenceId,
                                   final Long userId) {
        final InventoryTransaction transaction = new InventoryTransaction();
        transaction.setType(type);
        transaction.setProductId(productId);
        transaction.setLocationId(locationId);
        transaction.setLotId(lotId);
        transaction.setQuantity(quantity);
        transaction.setBeforeQuantity(before);
        transaction.setAfterQuantity(after);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(userId);

        transactionRepository.save(transaction);
    }

    private void validateRequiredIdentifiers(final Long productId, final Long locationId, final Long lotId) {
        if (productId == null) {
            throw new InvalidOperationException("Product id is required");
        }
        if (locationId == null) {
            throw new InvalidOperationException("Location id is required");
        }
        if (lotId == null) {
            throw new InvalidOperationException("Lot id is required");
        }
    }

    private void validateReferenceType(final String referenceType) {
        if (referenceType == null || referenceType.isBlank()) {
            throw new InvalidOperationException("Reference type is required");
        }
    }

    private void validateQuantity(final int quantity) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Quantity must be greater than zero");
        }
    }

    private void validateLotOwnership(final Long productId, final Long lotId) {
        final Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found: " + lotId));

        if (!productId.equals(lot.getProductId())) {
            throw new InvalidOperationException(
                    "Lot %d does not belong to product %d".formatted(lotId, productId));
        }
    }

    private int nullSafeQuantity(final Integer quantity) {
        return quantity == null ? 0 : quantity;
    }
}
