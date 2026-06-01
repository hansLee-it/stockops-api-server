package com.stockops.security;

import com.stockops.dto.ScopeAssignmentRequest;
import com.stockops.entity.Role;
import com.stockops.entity.User;
import com.stockops.entity.Warehouse;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds and validates effective scope metadata for users and roles.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
public class ScopeAccessService {

    private final CenterRepository centerRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Creates the service.
     *
     * @param centerRepository center repository
     * @param warehouseRepository warehouse repository
     */
    public ScopeAccessService(final CenterRepository centerRepository,
                              final WarehouseRepository warehouseRepository) {
        this.centerRepository = centerRepository;
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * Normalizes incoming scope assignments for persistence.
     *
     * @param requests requested assignments
     * @return validated assignments
     */
    @Transactional(readOnly = true)
    public Set<ScopeAssignment> normalizeAssignments(final Collection<ScopeAssignmentRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return new LinkedHashSet<>();
        }

        final LinkedHashSet<ScopeAssignment> assignments = new LinkedHashSet<>();
        for (ScopeAssignmentRequest request : requests) {
            assignments.add(normalizeAssignment(request));
        }

        if (assignments.stream().anyMatch(assignment -> assignment.getScope() == ScopeType.ADMIN)) {
            return new LinkedHashSet<>(List.of(ScopeAssignment.admin()));
        }

        return assignments;
    }

    /**
     * Builds effective scope metadata for a role.
     *
     * @param role role entity
     * @return effective role scope profile
     */
    @Transactional(readOnly = true)
    public ScopeAccessProfile buildRoleProfile(final Role role) {
        return buildProfile(role.getScopeAssignments());
    }

    /**
     * Builds effective scope metadata for a user.
     * Role and user scope assignments are merged, with empty assignments defaulting to global access
     * so existing permission-only users keep current visibility until constrained explicitly.
     *
     * @param user user entity
     * @return effective user scope profile
     */
    @Transactional(readOnly = true)
    public ScopeAccessProfile buildUserProfile(final User user) {
        final LinkedHashSet<ScopeAssignment> mergedAssignments = new LinkedHashSet<>();
        if (user.getRole() != null && user.getRole().getScopeAssignments() != null) {
            mergedAssignments.addAll(user.getRole().getScopeAssignments());
        }
        if (user.getScopeAssignments() != null) {
            mergedAssignments.addAll(user.getScopeAssignments());
        }
        return buildProfile(mergedAssignments);
    }

    private ScopeAccessProfile buildProfile(final Collection<ScopeAssignment> persistedAssignments) {
        final LinkedHashSet<ScopeAssignment> normalizedAssignments = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(persistedAssignments)) {
            persistedAssignments.stream()
                    .filter(Objects::nonNull)
                    .forEach(assignment -> normalizedAssignments.add(normalizeStoredAssignment(assignment)));
        }

        if (normalizedAssignments.isEmpty()
                || normalizedAssignments.stream().anyMatch(assignment -> assignment.getScope() == ScopeType.ADMIN)) {
            return new ScopeAccessProfile(true, List.of(ScopeAssignment.admin()), Set.of(), Set.of());
        }

        final LinkedHashSet<Long> centerIds = new LinkedHashSet<>();
        final LinkedHashSet<Long> warehouseIds = new LinkedHashSet<>();

        for (ScopeAssignment assignment : normalizedAssignments) {
            if (assignment.getCenterId() != null) {
                centerIds.add(assignment.getCenterId());
            }
            if (assignment.getWarehouseId() != null) {
                warehouseIds.add(assignment.getWarehouseId());
            }
        }

        return new ScopeAccessProfile(false, List.copyOf(normalizedAssignments), centerIds, warehouseIds);
    }

    private ScopeAssignment normalizeStoredAssignment(final ScopeAssignment assignment) {
        return normalizeAssignment(new ScopeAssignmentRequest(
                assignment.getScope(),
                assignment.getCenterId(),
                assignment.getWarehouseId()));
    }

    private ScopeAssignment normalizeAssignment(final ScopeAssignmentRequest request) {
        if (request.scope() == null) {
            throw new IllegalArgumentException("Scope is required");
        }
        return switch (request.scope()) {
            case ADMIN -> normalizeAdmin(request);
            case CENTER -> normalizeCenter(request);
            case WAREHOUSE -> normalizeWarehouse(request);
            case STORE -> normalizeStore(request);
        };
    }

    private ScopeAssignment normalizeAdmin(final ScopeAssignmentRequest request) {
        if (request.centerId() != null || request.warehouseId() != null) {
            throw new IllegalArgumentException("ADMIN scope cannot target a center or warehouse");
        }
        return ScopeAssignment.admin();
    }

    private ScopeAssignment normalizeCenter(final ScopeAssignmentRequest request) {
        if (request.centerId() == null) {
            throw new IllegalArgumentException("CENTER scope requires centerId");
        }
        if (request.warehouseId() != null) {
            throw new IllegalArgumentException("CENTER scope cannot target warehouseId");
        }
        if (!centerRepository.existsById(request.centerId())) {
            throw new ResourceNotFoundException("Center not found: " + request.centerId());
        }
        return new ScopeAssignment(ScopeType.CENTER, request.centerId(), null);
    }

    private ScopeAssignment normalizeWarehouse(final ScopeAssignmentRequest request) {
        if (request.warehouseId() == null) {
            throw new IllegalArgumentException("WAREHOUSE scope requires warehouseId");
        }

        final Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
        final Long resolvedCenterId = warehouse.getCenter().getId();

        if (request.centerId() != null && !request.centerId().equals(resolvedCenterId)) {
            throw new IllegalArgumentException("WAREHOUSE scope centerId must match the warehouse's center");
        }

        return new ScopeAssignment(ScopeType.WAREHOUSE, resolvedCenterId, request.warehouseId());
    }

    private ScopeAssignment normalizeStore(final ScopeAssignmentRequest request) {
        if (request.warehouseId() == null) {
            throw new IllegalArgumentException("STORE scope requires warehouseId");
        }

        final Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
        final Long resolvedCenterId = warehouse.getCenter().getId();

        if (request.centerId() != null && !request.centerId().equals(resolvedCenterId)) {
            throw new IllegalArgumentException("STORE scope centerId must match the store warehouse's center");
        }

        return new ScopeAssignment(ScopeType.STORE, resolvedCenterId, request.warehouseId());
    }
}
