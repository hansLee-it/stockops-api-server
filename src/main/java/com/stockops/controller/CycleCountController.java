package com.stockops.controller;

import com.stockops.dto.CompleteCycleCountRequest;
import com.stockops.dto.CreateCycleCountRequest;
import com.stockops.dto.CycleCountDTO;
import com.stockops.security.CurrentUserProvider;
import com.stockops.service.CycleCountService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Cycle count workflow API controller.
 * Allows managers to create, start, inspect, and complete location-based inventory counts.
 *
 * @author StockOps Team
 * @since 1.0
 * @see CycleCountService
 */
@RestController
@RequestMapping("/api/v1/cycle-counts")
public class CycleCountController {

    private final CycleCountService cycleCountService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates a new cycle count.
     *
     * @param request cycle count creation payload
     * @param principal authenticated principal
     * @return created cycle count response
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('CYCLE_COUNT_CREATE')")
    public ResponseEntity<CycleCountDTO> createCycleCount(@Valid @RequestBody final CreateCycleCountRequest request) {
        final CycleCountDTO created = cycleCountService.createCycleCount(request, currentUserProvider.getCurrentUserId());
        return ResponseEntity.created(URI.create("/api/v1/cycle-counts/" + created.id())).body(created);
    }

    /**
     * Lists cycle counts, newest first.
     *
     * @return cycle count list
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('CYCLE_COUNT_READ')")
    public ResponseEntity<List<CycleCountDTO>> listCycleCounts() {
        return ResponseEntity.ok(cycleCountService.listCycleCounts());
    }

    /**
     * Retrieves a cycle count by identifier.
     *
     * @param id cycle count identifier
     * @return cycle count response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CYCLE_COUNT_READ')")
    public ResponseEntity<CycleCountDTO> getCycleCount(@PathVariable final Long id) {
        return ResponseEntity.ok(cycleCountService.getCycleCount(id));
    }

    /**
     * Starts a pending cycle count.
     *
     * @param id cycle count identifier
     * @param principal authenticated principal
     * @return started cycle count response
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("@permissionChecker.hasPermission('CYCLE_COUNT_EXECUTE')")
    public ResponseEntity<CycleCountDTO> startCycleCount(@PathVariable final Long id) {
        return ResponseEntity.ok(cycleCountService.startCycleCount(id, currentUserProvider.getCurrentUserId()));
    }

    /**
     * Completes an in-progress cycle count with the final counted quantities.
     *
     * @param id cycle count identifier
     * @param request cycle count completion payload
     * @param principal authenticated principal
     * @return completed cycle count response
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("@permissionChecker.hasPermission('CYCLE_COUNT_EXECUTE')")
    public ResponseEntity<CycleCountDTO> completeCycleCount(@PathVariable final Long id,
                                                            @Valid @RequestBody final CompleteCycleCountRequest request) {
        return ResponseEntity.ok(cycleCountService.completeCycleCount(id, request, currentUserProvider.getCurrentUserId()));
    }

    public CycleCountController(final CycleCountService cycleCountService, final CurrentUserProvider currentUserProvider) {
        this.cycleCountService = cycleCountService;
        this.currentUserProvider = currentUserProvider;
    }
}
