package com.stockops.service;

import com.stockops.dto.CreateStockAdjustmentRequest;
import com.stockops.dto.StockAdjustmentDTO;
import com.stockops.entity.AuditLog;
import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryTransaction;
import com.stockops.entity.ReasonCode;
import com.stockops.entity.StockAdjustment;
import com.stockops.entity.User;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.config.MetricsConfig;
import com.stockops.inventory.WebSocketStockPublisher;
import com.stockops.repository.AuditLogRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.InventoryTransactionRepository;
import com.stockops.repository.ReasonCodeRepository;
import com.stockops.repository.StockAdjustmentRepository;
import com.stockops.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Handles stock adjustment request creation, approval, and history retrieval.
 * Maintains adjustment workflow state and records audit events for inventory corrections.
 *
 * @author StockOps Team
 * @since 1.0
 * @see StockAdjustmentRepository
 * @see InventoryRepository
 * @see AuditLogRepository
 */
@Service
public class StockAdjustmentService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String AUDIT_ENTITY_TYPE = "StockAdjustment";
    private static final String TRANSACTION_TYPE_ADJUSTMENT = "ADJUSTMENT";

    private final StockAdjustmentRepository adjustmentRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReasonCodeRepository reasonCodeRepository;
    private final UserRepository userRepository;
    private final WebSocketStockPublisher webSocketStockPublisher;
    private final MetricsConfig metricsConfig;

    /**
     * Creates a pending stock adjustment request.
     *
     * @param request requested quantity change
     * @param userId requesting operator identifier
     * @return created stock adjustment
     * @throws ResourceNotFoundException when the inventory row or reason code does not exist
     * @throws InvalidOperationException when the requested quantity is negative
     */
    @Transactional
    public StockAdjustmentDTO createAdjustment(final CreateStockAdjustmentRequest request, final Long userId) {
        if (request.newQuantity() < 0) {
            throw new InvalidOperationException("New quantity must be zero or greater");
        }

        final Inventory inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        final ReasonCode reasonCode = reasonCodeRepository.findById(request.reasonCodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Reason code not found"));

        final int beforeQuantity = nullSafeQuantity(inventory.getQuantity());
        final int difference = request.newQuantity() - beforeQuantity;

        final StockAdjustment adjustment = new StockAdjustment();
        adjustment.setInventoryId(request.inventoryId());
        adjustment.setBeforeQuantity(beforeQuantity);
        adjustment.setAfterQuantity(request.newQuantity());
        adjustment.setDifference(difference);
        adjustment.setReasonCodeId(reasonCode.getId());
        adjustment.setNote(request.note());
        adjustment.setStatus(STATUS_PENDING);
        adjustment.setCreatedBy(userId);

        final StockAdjustment saved = adjustmentRepository.save(adjustment);
        recordAuditLog(saved.getId(), "CREATE", String.valueOf(beforeQuantity), String.valueOf(request.newQuantity()), userId);
        return toDto(saved);
    }

    /**
     * Approves or rejects a pending adjustment request.
     * When approved, the inventory balance and transaction history are updated atomically.
     * Evicts all inventory and dashboard caches because stock levels have changed.
     *
     * @param adjustmentId adjustment identifier
     * @param approved approval decision
     * @param approverId approving operator identifier
     * @return processed stock adjustment
     * @throws ResourceNotFoundException when the adjustment, inventory, or reason code does not exist
     * @throws InvalidOperationException when the adjustment was already processed
     */
    @Transactional
    @CacheEvict(value = {"inventory", "dashboard::summary", "center::inventory"}, allEntries = true)
    public StockAdjustmentDTO approveAdjustment(final Long adjustmentId, final boolean approved, final Long approverId) {
        final StockAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found"));

        if (!STATUS_PENDING.equals(adjustment.getStatus())) {
            throw new InvalidOperationException("Adjustment already processed");
        }

        if (adjustment.getReasonCodeId() != null) {
            reasonCodeRepository.findById(adjustment.getReasonCodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reason code not found"));
        }

        if (approved) {
            final Inventory inventory = inventoryRepository.findByIdForUpdate(adjustment.getInventoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

            final int beforeQuantity = nullSafeQuantity(inventory.getQuantity());
            inventory.setQuantity(adjustment.getAfterQuantity());
            final Inventory savedInventory = inventoryRepository.save(inventory);

            recordTransaction(inventory, adjustmentId, beforeQuantity, adjustment.getAfterQuantity(), approverId);
            recordAuditLog(adjustmentId, "APPROVE", String.valueOf(beforeQuantity),
                    String.valueOf(adjustment.getAfterQuantity()), approverId);

            webSocketStockPublisher.publishStockChange(
                    "ADJUSTMENT",
                    inventory.getProductId(),
                    inventory.getLocationId(),
                    Math.abs(adjustment.getAfterQuantity() - beforeQuantity),
                    savedInventory.getQuantity());

            adjustment.setStatus(STATUS_APPROVED);
            metricsConfig.recordInventoryOperation("adjustment");
        } else {
            recordAuditLog(adjustmentId, "REJECT", STATUS_PENDING, STATUS_REJECTED, approverId);
            adjustment.setStatus(STATUS_REJECTED);
        }

        adjustment.setApprovedBy(approverId);
        return toDto(adjustmentRepository.save(adjustment));
    }

    /**
     * Retrieves a stock adjustment by identifier.
     *
     * @param id adjustment identifier
     * @return stock adjustment response
     * @throws ResourceNotFoundException when the adjustment does not exist
     */
    @Transactional(readOnly = true)
    public StockAdjustmentDTO getAdjustment(final Long id) {
        final StockAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found"));
        return toDto(adjustment);
    }

    /**
     * Retrieves all pending stock adjustment requests.
     *
     * @return pending adjustment list
     */
    @Transactional(readOnly = true)
    public List<StockAdjustmentDTO> getPendingAdjustments() {
        return adjustmentRepository.findByStatus(STATUS_PENDING).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retrieves adjustment history for an inventory row.
     *
     * @param inventoryId inventory identifier
     * @return adjustment history
     */
    @Transactional(readOnly = true)
    public List<StockAdjustmentDTO> getAdjustmentsByInventory(final Long inventoryId) {
        return adjustmentRepository.findByInventoryId(inventoryId).stream()
                .map(this::toDto)
                .toList();
    }

    private void recordTransaction(final Inventory inventory,
                                   final Long adjustmentId,
                                   final int beforeQuantity,
                                   final int afterQuantity,
                                   final Long approverId) {
        final InventoryTransaction transaction = new InventoryTransaction();
        transaction.setType(TRANSACTION_TYPE_ADJUSTMENT);
        transaction.setProductId(inventory.getProductId());
        transaction.setLocationId(inventory.getLocationId());
        transaction.setLotId(inventory.getLotId());
        transaction.setQuantity(Math.abs(afterQuantity - beforeQuantity));
        transaction.setBeforeQuantity(beforeQuantity);
        transaction.setAfterQuantity(afterQuantity);
        transaction.setReferenceId(adjustmentId);
        transaction.setCreatedBy(approverId);
        transactionRepository.save(transaction);
    }

    private void recordAuditLog(final Long entityId,
                                final String action,
                                final String oldValue,
                                final String newValue,
                                final Long userId) {
        final AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(AUDIT_ENTITY_TYPE);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setPerformedBy(userId);
        auditLog.setPerformedAt(Instant.now());
        auditLogRepository.save(auditLog);
    }

    private StockAdjustmentDTO toDto(final StockAdjustment adjustment) {
        final Inventory inventory = inventoryRepository.findById(adjustment.getInventoryId()).orElse(null);
        final ReasonCode reasonCode = adjustment.getReasonCodeId() == null
                ? null
                : reasonCodeRepository.findById(adjustment.getReasonCodeId()).orElse(null);

        return new StockAdjustmentDTO(
                adjustment.getId(),
                adjustment.getInventoryId(),
                buildInventoryInfo(inventory),
                nullSafeQuantity(adjustment.getBeforeQuantity()),
                nullSafeQuantity(adjustment.getAfterQuantity()),
                nullSafeQuantity(adjustment.getDifference()),
                adjustment.getReasonCodeId(),
                reasonCode == null ? null : reasonCode.getName(),
                adjustment.getNote(),
                adjustment.getStatus(),
                adjustment.getCreatedBy(),
                findUserName(adjustment.getCreatedBy()),
                adjustment.getApprovedBy(),
                findUserName(adjustment.getApprovedBy()),
                adjustment.getCreatedAt(),
                adjustment.getUpdatedAt());
    }

    private String buildInventoryInfo(final Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return "productId=%d, locationId=%d, lotId=%s"
                .formatted(inventory.getProductId(), inventory.getLocationId(), String.valueOf(inventory.getLotId()));
    }

    private String findUserName(final Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId)
                .map(User::getName)
                .orElse(null);
    }

    private int nullSafeQuantity(final Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    public StockAdjustmentService(final StockAdjustmentRepository adjustmentRepository, final InventoryRepository inventoryRepository, final InventoryTransactionRepository transactionRepository, final AuditLogRepository auditLogRepository, final ReasonCodeRepository reasonCodeRepository, final UserRepository userRepository, final WebSocketStockPublisher webSocketStockPublisher, final MetricsConfig metricsConfig) {
        this.adjustmentRepository = adjustmentRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.reasonCodeRepository = reasonCodeRepository;
        this.userRepository = userRepository;
        this.webSocketStockPublisher = webSocketStockPublisher;
        this.metricsConfig = metricsConfig;
    }
}
