package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.stockops.dto.InventoryDTO;
import com.stockops.dto.InventoryReportFilter;
import com.stockops.entity.Center;
import com.stockops.entity.Location;
import com.stockops.entity.Warehouse;
import com.stockops.exception.ForbiddenException;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.security.ScopeGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock
    private InventoryQueryService inventoryQueryService;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private ScopeGuard scopeGuard;

    @InjectMocks
    private PdfReportService pdfReportService;

    @Test
    void generateInventoryReportRejectsOutOfScopeFilter() {
        doThrow(new ForbiddenException("Access denied for warehouse: 11"))
                .when(scopeGuard).assertCenterWarehouseAccess(1L, 11L);

        assertThatThrownBy(() -> pdfReportService.generateInventoryReport(
                new InventoryReportFilter(null, null, 1L, 11L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied for warehouse: 11");
    }

    @Test
    void generateInventoryReportBuildsPdfForScopedRows() {
        final Center center = new Center();
        center.setId(1L);
        center.setName("Center A");

        final Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Warehouse A");
        warehouse.setCenter(center);

        final Location location = new Location();
        location.setId(100L);
        location.setName("Storage A");
        location.setWarehouse(warehouse);

        when(inventoryQueryService.getAllInventory()).thenReturn(List.of(new InventoryDTO(
                1L,
                101L,
                "P-101",
                "Scoped Product",
                100L,
                "LOC-100",
                "Storage A",
                1000L,
                "LOT-1",
                LocalDate.of(2026, 5, 1),
                5,
                0,
                "ACTIVE",
                Instant.parse("2026-04-21T00:00:00Z"),
                Instant.parse("2026-04-21T00:00:00Z"))));
        when(locationRepository.findAllById(any())).thenReturn(List.of(location));

        final byte[] pdf = pdfReportService.generateInventoryReport(new InventoryReportFilter(null, null, null, null));

        assertThat(pdf).isNotEmpty();
    }
}
