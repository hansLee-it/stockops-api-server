package com.stockops.service;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.dto.CreateOutboundRequest;
import com.stockops.dto.OutboundDTO;
import com.stockops.dto.OutboundItemDTO;
import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.Lot;
import com.stockops.entity.Outbound;
import com.stockops.entity.OutboundItem;
import com.stockops.entity.Product;
import com.stockops.exception.InsufficientStockException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.OutboundItemRepository;
import com.stockops.repository.OutboundRepository;
import com.stockops.repository.ProductRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbound registration and confirmation business logic.
 * Confirmation allocates lots using FEFO and records inventory deductions atomically.
 *
 * @author StockOps Team
 * @since 1.0
 * @see OutboundRepository
 * @see OutboundItemRepository
 * @see InventoryService
 */
@Service
@RequiredArgsConstructor
public class OutboundService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String OUTBOUND_REFERENCE_TYPE = "OUTBOUND";

    private final OutboundRepository outboundRepository;
    private final OutboundItemRepository outboundItemRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final LotRepository lotRepository;
    private final ProductRepository productRepository;

    /**
     * Creates a draft outbound header.
     *
     * @param request outbound creation payload
     * @param userId creator user id
     * @return created outbound response
     */
    @Transactional
    public OutboundDTO createOutbound(final CreateOutboundRequest request, final Long userId) {
        final Outbound outbound = new Outbound();
        outbound.setOutboundDate(request.outboundDate() != null ? request.outboundDate() : LocalDate.now());
        outbound.setCustomer(request.customer());
        outbound.setStatus(STATUS_DRAFT);
        outbound.setTotalQuantity(0);
        outbound.setCreatedBy(userId);

        return toDTO(outboundRepository.save(outbound));
    }

    /**
     * Adds a requested item to a draft outbound.
     * The concrete FEFO lot allocation is deferred until confirmation.
     *
     * @param outboundId outbound id
     * @param request outbound item payload
     * @return created draft outbound item
     * @throws ResourceNotFoundException when outbound does not exist
     * @throws InvalidOperationException when outbound is not draft or quantity is invalid
     */
    @Transactional
    public OutboundItemDTO addItem(final Long outboundId, final AddOutboundItemRequest request) {
        final Outbound outbound = findOutboundById(outboundId);
        validateDraft(outbound);
        final Product product = findProductById(request.productId());

        if (request.quantity() <= 0) {
            throw new InvalidOperationException("Quantity must be greater than zero");
        }

        final OutboundItem item = new OutboundItem();
        item.setOutboundId(outboundId);
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());

        final OutboundItem savedItem = outboundItemRepository.save(item);
        outbound.setTotalQuantity(nullSafeQuantity(outbound.getTotalQuantity()) + request.quantity());
        outboundRepository.save(outbound);

        return toItemDTO(savedItem, product.getName(), null);
    }

    /**
     * Confirms a draft outbound.
     * Requested quantities are allocated to lots in FEFO order and inventory is decreased in the same transaction.
     *
     * @param outboundId outbound id
     * @param userId operator user id
     * @return confirmed outbound response
     * @throws ResourceNotFoundException when outbound does not exist
     * @throws InvalidOperationException when outbound is not draft or has no items
     * @throws InsufficientStockException when requested stock cannot be fully allocated
     */
    @Transactional
    public OutboundDTO confirmOutbound(final Long outboundId, final Long userId) {
        final Outbound outbound = findOutboundById(outboundId);
        validateDraft(outbound);

        final List<OutboundItem> requestedItems = outboundItemRepository.findByOutboundId(outboundId);
        if (requestedItems.isEmpty()) {
            throw new InvalidOperationException("Cannot confirm outbound with no items");
        }

        for (final OutboundItem requestedItem : requestedItems) {
            final List<OutboundItem> allocatedItems = allocateInventoryByFefo(requestedItem, outboundId, userId);
            outboundItemRepository.delete(requestedItem);
            outboundItemRepository.saveAll(allocatedItems);
        }

        outbound.setStatus(STATUS_CONFIRMED);
        return toDTO(outboundRepository.save(outbound));
    }

    /**
     * Returns an outbound header by id.
     *
     * @param id outbound id
     * @return outbound response
     * @throws ResourceNotFoundException when outbound does not exist
     */
    @Transactional(readOnly = true)
    public OutboundDTO getOutbound(final Long id) {
        return toDTO(findOutboundById(id));
    }

    /**
     * Returns outbound items for the supplied outbound id.
     *
     * @param outboundId outbound id
     * @return outbound item list
     * @throws ResourceNotFoundException when outbound does not exist
     */
    @Transactional(readOnly = true)
    public List<OutboundItemDTO> getOutboundItems(final Long outboundId) {
        findOutboundById(outboundId);

        final List<OutboundItem> items = outboundItemRepository.findByOutboundId(outboundId);
        final Map<Long, String> productNames = loadProductNames(items);
        final Map<Long, String> lotNumbers = loadLotNumbers(items);

        return items.stream()
                .map(item -> toItemDTO(item, productNames.get(item.getProductId()), lotNumbers.get(item.getLotId())))
                .toList();
    }

    private List<OutboundItem> allocateInventoryByFefo(final OutboundItem requestedItem,
                                                       final Long outboundId,
                                                       final Long userId) {
        int remainingQuantity = nullSafeQuantity(requestedItem.getQuantity());
        final List<Lot> lots = lotRepository.findByProductIdOrderByExpiryDateAsc(requestedItem.getProductId());
        final List<OutboundItem> allocatedItems = new ArrayList<>();

        for (final Lot lot : lots) {
            if (remainingQuantity <= 0) {
                break;
            }

            final List<Inventory> inventories = inventoryRepository
                    .findByProductIdAndLotIdAndStatusAndQuantityGreaterThanOrderByLocationIdAsc(
                            requestedItem.getProductId(), lot.getId(), InventoryStatus.ACTIVE, 0);

            for (final Inventory inventory : inventories) {
                if (remainingQuantity <= 0) {
                    break;
                }

                final int availableQuantity = nullSafeQuantity(inventory.getQuantity());
                if (availableQuantity <= 0) {
                    continue;
                }

                final int deductedQuantity = Math.min(remainingQuantity, availableQuantity);
                inventoryService.decreaseStock(
                        requestedItem.getProductId(),
                        inventory.getLocationId(),
                        lot.getId(),
                        deductedQuantity,
                        OUTBOUND_REFERENCE_TYPE,
                        outboundId,
                        userId);

                allocatedItems.add(createAllocatedItem(outboundId, requestedItem.getProductId(), lot.getId(), deductedQuantity));
                remainingQuantity -= deductedQuantity;
            }
        }

        if (remainingQuantity > 0) {
            throw new InsufficientStockException(
                    requestedItem.getProductId(),
                    nullSafeQuantity(requestedItem.getQuantity()),
                    nullSafeQuantity(requestedItem.getQuantity()) - remainingQuantity);
        }

        return allocatedItems;
    }

    private Outbound findOutboundById(final Long outboundId) {
        return outboundRepository.findById(outboundId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbound not found: " + outboundId));
    }

    private void validateDraft(final Outbound outbound) {
        if (!STATUS_DRAFT.equals(outbound.getStatus())) {
            throw new InvalidOperationException("Cannot modify a non-draft outbound");
        }
    }

    private Map<Long, String> loadProductNames(final List<OutboundItem> items) {
        final Set<Long> productIds = items.stream()
                .map(OutboundItem::getProductId)
                .collect(Collectors.toSet());

        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, String> productNames = new HashMap<>();
        for (final Product product : productRepository.findAllById(productIds)) {
            productNames.put(product.getId(), product.getName());
        }
        return productNames;
    }

    private Map<Long, String> loadLotNumbers(final List<OutboundItem> items) {
        final Set<Long> lotIds = items.stream()
                .map(OutboundItem::getLotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (lotIds.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, String> lotNumbers = new HashMap<>();
        for (final Lot lot : lotRepository.findAllById(lotIds)) {
            lotNumbers.put(lot.getId(), lot.getLotNumber());
        }
        return lotNumbers;
    }

    private Product findProductById(final Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private OutboundItem createAllocatedItem(final Long outboundId,
                                             final Long productId,
                                             final Long lotId,
                                             final int quantity) {
        final OutboundItem allocatedItem = new OutboundItem();
        allocatedItem.setOutboundId(outboundId);
        allocatedItem.setProductId(productId);
        allocatedItem.setLotId(lotId);
        allocatedItem.setQuantity(quantity);
        return allocatedItem;
    }

    private OutboundDTO toDTO(final Outbound outbound) {
        return new OutboundDTO(
                outbound.getId(),
                outbound.getOutboundDate(),
                outbound.getCustomer(),
                outbound.getStatus(),
                nullSafeQuantity(outbound.getTotalQuantity()),
                outbound.getCreatedBy(),
                outbound.getCreatedAt(),
                outbound.getUpdatedAt());
    }

    private OutboundItemDTO toItemDTO(final OutboundItem item,
                                      final String productName,
                                      final String lotNumber) {
        return new OutboundItemDTO(
                item.getId(),
                item.getOutboundId(),
                item.getProductId(),
                productName,
                item.getLotId(),
                lotNumber,
                nullSafeQuantity(item.getQuantity()),
                item.getCreatedAt());
    }

    private int nullSafeQuantity(final Integer quantity) {
        return quantity == null ? 0 : quantity;
    }
}
