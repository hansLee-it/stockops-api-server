package com.stockops.controller;

import com.stockops.dto.ControllerCommandRequest;
import com.stockops.dto.ControllerCommandResponse;
import com.stockops.service.ControllerCommandService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller command bridge API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/environment/controllers/{controllerId}/commands")
public class ControllerCommandController {

    private final ControllerCommandService controllerCommandService;

    /**
     * Creates the controller.
     *
     * @param controllerCommandService controller command service
     */
    public ControllerCommandController(final ControllerCommandService controllerCommandService) {
        this.controllerCommandService = controllerCommandService;
    }

    /**
     * Sends a command to Sensimul and records the audit row.
     *
     * @param controllerId environment controller identifier
     * @param request command payload
     * @return accepted command response
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_COMMAND')")
    public ResponseEntity<ControllerCommandResponse> sendCommand(
            @PathVariable final Long controllerId,
            @Valid @RequestBody final ControllerCommandRequest request) {
        return ResponseEntity.accepted().body(controllerCommandService.sendCommand(controllerId, request));
    }

    /**
     * Returns recent command history for a controller.
     *
     * @param controllerId environment controller identifier
     * @param pageable paging parameters
     * @return recent command history
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_COMMAND')")
    public ResponseEntity<List<ControllerCommandResponse>> getCommandHistory(
            @PathVariable final Long controllerId,
            @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(controllerCommandService.getCommandHistory(controllerId, pageable));
    }
}
