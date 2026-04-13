package com.stockops.controller;

import com.stockops.dto.CreateReasonCodeRequest;
import com.stockops.dto.ReasonCodeDTO;
import com.stockops.dto.UpdateReasonCodeRequest;
import com.stockops.service.ReasonCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Reason code management API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/reason-codes")
public class ReasonCodeController {

    private final ReasonCodeService reasonCodeService;

    /**
     * Creates the controller.
     *
     * @param reasonCodeService reason code service
     */
    public ReasonCodeController(final ReasonCodeService reasonCodeService) {
        this.reasonCodeService = reasonCodeService;
    }

    /**
     * Creates a reason code.
     *
     * @param request creation request
     * @return created reason code
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('REASON_CODE_CREATE')")
    public ResponseEntity<ReasonCodeDTO> createReasonCode(@Valid @RequestBody final CreateReasonCodeRequest request) {
        final ReasonCodeDTO created = reasonCodeService.createReasonCode(request);
        return ResponseEntity.created(URI.create("/api/v1/reason-codes/" + created.id())).body(created);
    }

    /**
     * Gets a reason code by id.
     *
     * @param id reason code id
     * @return reason code response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('REASON_CODE_READ')")
    public ResponseEntity<ReasonCodeDTO> getReasonCode(@PathVariable final Long id) {
        return ResponseEntity.ok(reasonCodeService.getReasonCodeById(id));
    }

    /**
     * Lists reason codes.
     *
     * @param category optional category filter
     * @return reason code list
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('REASON_CODE_READ')")
    public ResponseEntity<List<ReasonCodeDTO>> getAllReasonCodes(@RequestParam(required = false) final String category) {
        if (category != null) {
            return ResponseEntity.ok(reasonCodeService.getReasonCodesByCategory(category));
        }

        return ResponseEntity.ok(reasonCodeService.getAllReasonCodes());
    }

    /**
     * Updates a reason code.
     *
     * @param id reason code id
     * @param request update request
     * @return updated reason code
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('REASON_CODE_UPDATE')")
    public ResponseEntity<ReasonCodeDTO> updateReasonCode(@PathVariable final Long id,
                                                          @Valid @RequestBody final UpdateReasonCodeRequest request) {
        return ResponseEntity.ok(reasonCodeService.updateReasonCode(id, request));
    }

    /**
     * Deletes a reason code.
     *
     * @param id reason code id
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('REASON_CODE_DELETE')")
    public ResponseEntity<Void> deleteReasonCode(@PathVariable final Long id) {
        reasonCodeService.deleteReasonCode(id);
        return ResponseEntity.noContent().build();
    }
}
