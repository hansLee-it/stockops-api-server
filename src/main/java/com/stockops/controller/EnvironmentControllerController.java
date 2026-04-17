package com.stockops.controller;

import com.stockops.dto.EnvironmentControllerRequest;
import com.stockops.dto.EnvironmentControllerResponse;
import com.stockops.service.EnvironmentControllerService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Environment controller lifecycle API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/environment/controllers")
public class EnvironmentControllerController {

    private final EnvironmentControllerService environmentControllerService;

    /**
     * Creates the controller.
     *
     * @param environmentControllerService environment controller service
     */
    public EnvironmentControllerController(final EnvironmentControllerService environmentControllerService) {
        this.environmentControllerService = environmentControllerService;
    }

    /**
     * Creates an environment controller.
     *
     * @param request creation payload
     * @return created controller response
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<EnvironmentControllerResponse> createEnvironmentController(
            @Valid @RequestBody final EnvironmentControllerRequest request) {
        final EnvironmentControllerResponse controller =
                environmentControllerService.createEnvironmentController(request);
        return ResponseEntity.created(URI.create("/api/v1/environment/controllers/" + controller.id())).body(controller);
    }

    /**
     * Returns active environment controllers.
     *
     * @param pageable paging parameters
     * @return paged active controllers
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<Page<EnvironmentControllerResponse>> getEnvironmentControllers(
            @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(environmentControllerService.getEnvironmentControllers(pageable));
    }

    /**
     * Returns an active environment controller by id.
     *
     * @param id controller identifier
     * @return controller response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<EnvironmentControllerResponse> getEnvironmentController(@PathVariable final Long id) {
        return ResponseEntity.ok(environmentControllerService.getEnvironmentControllerById(id));
    }

    /**
     * Updates an active environment controller.
     *
     * @param id controller identifier
     * @param request update payload
     * @return updated controller response
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<EnvironmentControllerResponse> updateEnvironmentController(
            @PathVariable final Long id,
            @Valid @RequestBody final EnvironmentControllerRequest request) {
        return ResponseEntity.ok(environmentControllerService.updateEnvironmentController(id, request));
    }

    /**
     * Soft-deletes an active environment controller.
     *
     * @param id controller identifier
     * @return no-content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<Void> deleteEnvironmentController(@PathVariable final Long id) {
        environmentControllerService.deleteEnvironmentController(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a soft-deleted environment controller.
     *
     * @param id controller identifier
     * @return reactivated controller response
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<EnvironmentControllerResponse> reactivateEnvironmentController(@PathVariable final Long id) {
        return ResponseEntity.ok(environmentControllerService.reactivateEnvironmentController(id));
    }

    /**
     * Looks up an active environment controller by external identifiers.
     *
     * @param siteId Sensimul site identifier
     * @param controllerId Sensimul controller identifier
     * @return controller response
     */
    @GetMapping("/external/{siteId}/{controllerId}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<EnvironmentControllerResponse> getEnvironmentControllerByExternalIds(
            @PathVariable final String siteId,
            @PathVariable final String controllerId) {
        return ResponseEntity.ok(
                environmentControllerService.getEnvironmentControllerByExternalIds(siteId, controllerId));
    }
}
