package com.stockops.controller;

import com.stockops.dto.ApproveStockAdjustmentRequest;
import com.stockops.dto.CreateStockAdjustmentRequest;
import com.stockops.dto.StockAdjustmentDTO;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.UserRepository;
import com.stockops.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * Stock adjustment workflow API controller.
 * Accepts cycle-count driven balance correction requests and approval decisions.
 *
 * @author StockOps Team
 * @since 1.0
 * @see StockAdjustmentService
 */
@RestController
@RequestMapping("/api/v1/inventory/adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService adjustmentService;
    private final UserRepository userRepository;

    /**
     * Creates a new stock adjustment request.
     *
     * @param request adjustment creation payload
     * @param principal authenticated principal
     * @return created adjustment response
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_ADJUST_CREATE')")
    public ResponseEntity<StockAdjustmentDTO> createAdjustment(@Valid @RequestBody final CreateStockAdjustmentRequest request,
                                                               final Principal principal) {
        final Long userId = getCurrentUserId(principal);
        final StockAdjustmentDTO created = adjustmentService.createAdjustment(request, userId);
        return ResponseEntity.created(URI.create("/api/v1/inventory/adjustments/" + created.id())).body(created);
    }

    /**
     * Approves or rejects a pending stock adjustment request.
     *
     * @param id adjustment identifier
     * @param request approval payload
     * @param principal authenticated principal
     * @return processed adjustment response
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_ADJUST_APPROVE')")
    public ResponseEntity<StockAdjustmentDTO> approveAdjustment(@PathVariable final Long id,
                                                                @Valid @RequestBody final ApproveStockAdjustmentRequest request,
                                                                final Principal principal) {
        if (!id.equals(request.adjustmentId())) {
            throw new InvalidOperationException("Path id and adjustment id must match");
        }

        final Long approverId = getCurrentUserId(principal);
        return ResponseEntity.ok(adjustmentService.approveAdjustment(id, request.approved(), approverId));
    }

    /**
     * Retrieves a stock adjustment by identifier.
     *
     * @param id adjustment identifier
     * @return stock adjustment response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_ADJUST_READ')")
    public ResponseEntity<StockAdjustmentDTO> getAdjustment(@PathVariable final Long id) {
        return ResponseEntity.ok(adjustmentService.getAdjustment(id));
    }

    /**
     * Retrieves all pending stock adjustment requests.
     *
     * @return pending adjustment list
     */
    @GetMapping("/pending")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_ADJUST_APPROVE')")
    public ResponseEntity<List<StockAdjustmentDTO>> getPendingAdjustments() {
        return ResponseEntity.ok(adjustmentService.getPendingAdjustments());
    }

    /**
     * Retrieves adjustment history for an inventory row.
     *
     * @param inventoryId inventory identifier
     * @return adjustment history list
     */
    @GetMapping("/inventory/{inventoryId}")
    @PreAuthorize("@permissionChecker.hasPermission('INVENTORY_ADJUST_READ')")
    public ResponseEntity<List<StockAdjustmentDTO>> getAdjustmentsByInventory(@PathVariable final Long inventoryId) {
        return ResponseEntity.ok(adjustmentService.getAdjustmentsByInventory(inventoryId));
    }

    private Long getCurrentUserId(final Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new InvalidOperationException("Authenticated user is required");
        }

        return userRepository.findByEmail(principal.getName())
                .map(user -> user.getId())
                .orElseThrow(() -> new InvalidOperationException("Authenticated user not found"));
    }

    public StockAdjustmentController(final StockAdjustmentService adjustmentService, final UserRepository userRepository) {
        this.adjustmentService = adjustmentService;
        this.userRepository = userRepository;
    }
}
