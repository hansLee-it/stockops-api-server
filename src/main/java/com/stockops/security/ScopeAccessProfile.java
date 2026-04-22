package com.stockops.security;

import com.stockops.dto.ScopeAssignmentDTO;
import com.stockops.dto.ScopeMetadataDTO;

import java.util.List;
import java.util.Set;

/**
 * Immutable effective scope projection used by authentication and authorization.
 *
 * @author StockOps Team
 * @since 2.0
 */
public record ScopeAccessProfile(
        boolean global,
        List<ScopeAssignment> assignments,
        Set<Long> centerIds,
        Set<Long> warehouseIds
) {

    /**
     * Returns whether the profile can access the provided center.
     *
     * @param centerId center identifier
     * @return {@code true} when allowed
     */
    public boolean canAccessCenter(final Long centerId) {
        return global || centerId == null || centerIds.contains(centerId);
    }

    /**
     * Returns whether the profile can access the provided warehouse.
     *
     * @param warehouseId warehouse identifier
     * @return {@code true} when allowed
     */
    public boolean canAccessWarehouse(final Long warehouseId) {
        return global || warehouseId == null || warehouseIds.contains(warehouseId);
    }

    /**
     * Converts the profile into an API payload.
     *
     * @return scope metadata DTO
     */
    public ScopeMetadataDTO toDto() {
        final List<ScopeAssignmentDTO> assignmentDtos = assignments.stream()
                .map(assignment -> new ScopeAssignmentDTO(
                        assignment.getScope(),
                        assignment.getCenterId(),
                        assignment.getWarehouseId()))
                .toList();

        final List<Long> visibleCenters = centerIds.stream().sorted().toList();
        final List<Long> visibleWarehouses = warehouseIds.stream().sorted().toList();

        return new ScopeMetadataDTO(global, assignmentDtos, visibleCenters, visibleWarehouses);
    }
}
