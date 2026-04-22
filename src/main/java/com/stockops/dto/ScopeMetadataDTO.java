package com.stockops.dto;

import java.util.List;

/**
 * Effective scope metadata returned to API clients.
 *
 * @param global whether the subject can see all data
 * @param assignments normalized scope assignments
 * @param centerIds visible center ids
 * @param warehouseIds visible warehouse ids
 * @author StockOps Team
 * @since 2.0
 */
public record ScopeMetadataDTO(
        boolean global,
        List<ScopeAssignmentDTO> assignments,
        List<Long> centerIds,
        List<Long> warehouseIds
) {
}
