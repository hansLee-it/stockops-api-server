package com.stockops.controller;

import com.stockops.dto.RoleDTO;
import com.stockops.dto.RoleRequest;
import com.stockops.service.RoleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Role management API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    /**
     * Creates the controller.
     *
     * @param roleService role service
     */
    public RoleController(final RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Lists all roles.
     *
     * @return role list
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('ROLE_READ')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * Gets a role by id.
     *
     * @param id role id
     * @return role response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ROLE_READ')")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable final Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    /**
     * Creates a role.
     *
     * @param request role creation request
     * @return created role
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('ROLE_CREATE')")
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody final RoleRequest request) {
        return ResponseEntity.status(201)
                .body(roleService.createRole(request.name(), request.description(), request.scopeAssignments()));
    }

    /**
     * Updates a role.
     *
     * @param id role id
     * @param request role update request
     * @return updated role
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ROLE_UPDATE')")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable final Long id,
                                              @Valid @RequestBody final RoleRequest request) {
        return ResponseEntity.ok(
                roleService.updateRole(id, request.name(), request.description(), request.scopeAssignments()));
    }

    /**
     * Deletes a role.
     *
     * @param id role id
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ROLE_DELETE')")
    public ResponseEntity<Void> deleteRole(@PathVariable final Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
