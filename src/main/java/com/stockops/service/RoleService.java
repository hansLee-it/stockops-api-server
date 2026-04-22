package com.stockops.service;

import com.stockops.dto.RoleDTO;
import com.stockops.dto.ScopeMetadataDTO;
import com.stockops.dto.ScopeAssignmentRequest;
import com.stockops.entity.Role;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.RoleRepository;
import com.stockops.security.ScopeAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Role management business logic.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final ScopeAccessService scopeAccessService;

    /**
     * Creates the service.
     *
     * @param roleRepository role repository
     */
    public RoleService(final RoleRepository roleRepository,
                       final ScopeAccessService scopeAccessService) {
        this.roleRepository = roleRepository;
        this.scopeAccessService = scopeAccessService;
    }

    /**
     * Creates a new role.
     *
     * @param name role name
     * @param description role description
     * @return created role
     */
    @Transactional
    public RoleDTO createRole(final String name,
                              final String description,
                              final List<ScopeAssignmentRequest> scopeAssignments) {
        final Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setScopeAssignments(scopeAccessService.normalizeAssignments(scopeAssignments));
        return toDto(roleRepository.save(role));
    }

    /**
     * Retrieves a role by identifier.
     *
     * @param id role identifier
     * @return role response
     */
    @Transactional(readOnly = true)
    public RoleDTO getRoleById(final Long id) {
        return toDto(findRoleEntityById(id));
    }

    /**
     * Retrieves all roles.
     *
     * @return role list
     */
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Retrieves a role entity by name.
     *
     * @param name role name
     * @return role entity
     */
    @Transactional(readOnly = true)
    public Role getRoleByName(final String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
    }

    /**
     * Updates an existing role.
     *
     * @param id role identifier
     * @param name new name
     * @param description new description
     * @return updated role response
     */
    @Transactional
    public RoleDTO updateRole(final Long id,
                              final String name,
                              final String description,
                              final List<ScopeAssignmentRequest> scopeAssignments) {
        final Role role = findRoleEntityById(id);
        role.setName(name);
        role.setDescription(description);
        role.setScopeAssignments(scopeAccessService.normalizeAssignments(scopeAssignments));
        return toDto(roleRepository.save(role));
    }

    /**
     * Deletes a role.
     *
     * @param id role identifier
     */
    @Transactional
    public void deleteRole(final Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role not found: " + id);
        }

        roleRepository.deleteById(id);
    }

    private Role findRoleEntityById(final Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private RoleDTO toDto(final Role role) {
        final ScopeMetadataDTO scopeMetadata = scopeAccessService.buildRoleProfile(role).toDto();
        return new RoleDTO(role.getId(), role.getName(), role.getDescription(), scopeMetadata, role.getCreatedAt());
    }
}
