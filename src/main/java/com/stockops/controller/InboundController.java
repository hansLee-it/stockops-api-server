package com.stockops.controller;

import com.stockops.dto.AddInboundItemRequest;
import com.stockops.dto.CreateInboundRequest;
import com.stockops.dto.InboundDTO;
import com.stockops.dto.InboundItemDTO;
import com.stockops.service.InboundService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound registration API controller.
 * Exposes endpoints to draft, populate, confirm, and inspect inbound receipts.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/inbounds")
public class InboundController {

    private final InboundService inboundService;
    private final com.stockops.security.CurrentUserProvider currentUserProvider;

    /**
     * Returns all inbounds, optionally filtered by status.
     *
     * @param status optional status filter (DRAFT, CONFIRMED)
     * @return list of inbound DTOs
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_READ')")
    public ResponseEntity<List<InboundDTO>> getAllInbounds(@RequestParam(required = false) final String status) {
        if (status != null) {
            return ResponseEntity.ok(inboundService.getInboundsByStatus(status));
        }
        return ResponseEntity.ok(inboundService.getAllInbounds());
    }

    /**
     * Creates a draft inbound.
     *
     * @param request inbound creation payload
     * @param principal authenticated principal
     * @return created inbound DTO
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_CREATE')")
    public ResponseEntity<InboundDTO> createInbound(@Valid @RequestBody final CreateInboundRequest request) {
        final InboundDTO inbound = inboundService.createInbound(request, currentUserProvider.getCurrentUserId());
        return ResponseEntity.created(URI.create("/api/v1/inbounds/" + inbound.id())).body(inbound);
    }

    /**
     * Adds a draft item to an inbound.
     *
     * @param id inbound identifier
     * @param request item creation payload
     * @return created item DTO
     */
    @PostMapping("/{id}/items")
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_CREATE')")
    public ResponseEntity<InboundItemDTO> addItem(@PathVariable final Long id,
                                                  @Valid @RequestBody final AddInboundItemRequest request) {
        return ResponseEntity.ok(inboundService.addItem(id, request));
    }

    /**
     * Confirms a draft inbound.
     *
     * @param id inbound identifier
     * @param principal authenticated principal
     * @return confirmed inbound DTO
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_CONFIRM')")
    public ResponseEntity<InboundDTO> confirmInbound(@PathVariable final Long id) {
        return ResponseEntity.ok(inboundService.confirmInbound(id, currentUserProvider.getCurrentUserId()));
    }

    /**
     * Returns an inbound header.
     *
     * @param id inbound identifier
     * @return inbound DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_READ')")
    public ResponseEntity<InboundDTO> getInbound(@PathVariable final Long id) {
        return ResponseEntity.ok(inboundService.getInbound(id));
    }

    /**
     * Returns inbound items for a header.
     *
     * @param id inbound identifier
     * @return inbound item DTOs
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("@permissionChecker.hasPermission('INBOUND_READ')")
    public ResponseEntity<List<InboundItemDTO>> getInboundItems(@PathVariable final Long id) {
        return ResponseEntity.ok(inboundService.getInboundItems(id));
    }

    public InboundController(final InboundService inboundService, final com.stockops.security.CurrentUserProvider currentUserProvider) {
        this.inboundService = inboundService;
        this.currentUserProvider = currentUserProvider;
    }
}
