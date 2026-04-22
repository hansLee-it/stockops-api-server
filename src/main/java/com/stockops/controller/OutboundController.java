package com.stockops.controller;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.dto.CreateOutboundRequest;
import com.stockops.dto.OutboundDTO;
import com.stockops.dto.OutboundItemDTO;
import com.stockops.service.OutboundService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
 * Outbound registration API controller.
 * Supports draft creation, item registration, FEFO confirmation, and outbound detail queries.
 * Detail access and inventory-consuming mutations are validated against the caller's effective scope.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/outbounds")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundService outboundService;
    private final com.stockops.security.CurrentUserProvider currentUserProvider;

    /**
     * Returns a list of outbounds, optionally filtered by status.
     *
     * @param status optional status filter (DRAFT, CONFIRMED)
     * @return list of outbound responses
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_READ')")
    public ResponseEntity<List<OutboundDTO>> getOutbounds(
            @RequestParam(required = false) final String status) {
        return ResponseEntity.ok(outboundService.getOutbounds(status));
    }

    /**
     * Creates a draft outbound.
     *
     * @param request outbound creation payload
     * @param principal authenticated principal
     * @return created outbound response
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_CREATE')")
    public ResponseEntity<OutboundDTO> createOutbound(@Valid @RequestBody final CreateOutboundRequest request) {
        final OutboundDTO outbound = outboundService.createOutbound(request, currentUserProvider.getCurrentUserId());
        return ResponseEntity.created(URI.create("/api/v1/outbounds/" + outbound.id())).body(outbound);
    }

    /**
     * Adds a draft item to an outbound.
     *
     * @param id outbound id
     * @param request outbound item payload
     * @return created outbound item response
     */
    @PostMapping("/{id}/items")
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_CREATE')")
    public ResponseEntity<OutboundItemDTO> addItem(@PathVariable final Long id,
                                                   @Valid @RequestBody final AddOutboundItemRequest request) {
        return ResponseEntity.ok(outboundService.addItem(id, request));
    }

    /**
     * Confirms a draft outbound using FEFO inventory allocation.
     *
     * @param id outbound id
     * @param principal authenticated principal
     * @return confirmed outbound response
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_CONFIRM')")
    public ResponseEntity<OutboundDTO> confirmOutbound(@PathVariable final Long id) {
        return ResponseEntity.ok(outboundService.confirmOutbound(id, currentUserProvider.getCurrentUserId()));
    }

    /**
     * Gets an outbound header.
     *
     * @param id outbound id
     * @return outbound response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_READ')")
    public ResponseEntity<OutboundDTO> getOutbound(@PathVariable final Long id) {
        return ResponseEntity.ok(outboundService.getOutbound(id));
    }

    /**
     * Gets outbound items.
     *
     * @param id outbound id
     * @return outbound item responses
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("@permissionChecker.hasPermission('OUTBOUND_READ')")
    public ResponseEntity<List<OutboundItemDTO>> getOutboundItems(@PathVariable final Long id) {
        return ResponseEntity.ok(outboundService.getOutboundItems(id));
    }
}
