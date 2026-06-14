package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.entity.Center;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.PurchaseOrderStatus;
import com.stockops.entity.Store;
import com.stockops.entity.User;
import com.stockops.entity.Warehouse;
import com.stockops.exception.ForbiddenException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.InboundItemRepository;
import com.stockops.repository.InboundRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.PurchaseOrderItemRepository;
import com.stockops.repository.PurchaseOrderRepository;
import com.stockops.repository.PurchaseOrderShipmentItemRepository;
import com.stockops.repository.PurchaseOrderShipmentRepository;
import com.stockops.repository.StoreRepository;
import com.stockops.security.CurrentUserProvider;
import com.stockops.security.ScopeGuard;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private PurchaseOrderShipmentRepository shipmentRepository;

    @Mock
    private PurchaseOrderShipmentItemRepository shipmentItemRepository;

    @Mock
    private CenterService centerService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ScopeGuard scopeGuard;

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private InboundItemRepository inboundItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Test
    void findByCenterIdReturnsEmptyListWhenCenterIsOutOfScope() {
        when(scopeGuard.filterCenterIds(List.of(2L))).thenReturn(List.of());

        assertThat(purchaseOrderService.findByCenterId(2L)).isEmpty();
        verify(purchaseOrderRepository, never()).findByRequestingCenterId(2L);
    }

    @Test
    void findByIdRejectsDirectAccessOutsideScope() {
        final Center center = new Center();
        center.setId(2L);

        final Warehouse warehouse = new Warehouse();
        warehouse.setId(20L);
        warehouse.setCenter(center);

        final PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(9L);
        purchaseOrder.setRequestingCenter(center);
        purchaseOrder.setTargetWarehouse(warehouse);

        when(purchaseOrderRepository.findById(9L)).thenReturn(Optional.of(purchaseOrder));
        org.mockito.Mockito.doThrow(new ForbiddenException("Access denied for warehouse: 20"))
                .when(scopeGuard).assertCenterWarehouseAccess(2L, 20L);

        assertThatThrownBy(() -> purchaseOrderService.findById(9L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied for warehouse: 20");
    }

    @Test
    void createStoreRequestRequiresStoreMembership() {
        final User noStore = new User();
        noStore.setId(5L);

        assertThatThrownBy(() -> purchaseOrderService.createStoreRequest(noStore))
                .isInstanceOf(InvalidOperationException.class);
        verify(purchaseOrderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createStoreRequestCreatesDraftBoundToStore() {
        final User storeUser = new User();
        storeUser.setId(5L);
        storeUser.setStoreId(3L);
        final Store store = new Store();
        store.setId(3L);
        when(storeRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(store));
        when(purchaseOrderRepository.countByPoNumberStartingWith(org.mockito.ArgumentMatchers.anyString())).thenReturn(0L);
        when(purchaseOrderRepository.save(org.mockito.ArgumentMatchers.any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final PurchaseOrder created = purchaseOrderService.createStoreRequest(storeUser);

        assertThat(created.getRequestingStore()).isSameAs(store);
        assertThat(created.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(created.getRequestingCenter()).isNull();
    }

    @Test
    void approveStoreRequestDesignatesCenterWarehouseAndAccepts() {
        final Store store = new Store();
        store.setId(3L);
        final PurchaseOrder po = new PurchaseOrder();
        po.setId(9L);
        po.setRequestingStore(store);
        po.setStatus(PurchaseOrderStatus.REQUESTED);
        when(purchaseOrderRepository.findById(9L)).thenReturn(Optional.of(po));

        final Center center = new Center();
        center.setId(2L);
        final Warehouse warehouse = new Warehouse();
        warehouse.setId(20L);
        warehouse.setCenter(center);
        when(centerService.findById(2L)).thenReturn(center);
        when(warehouseService.findById(20L)).thenReturn(warehouse);
        when(purchaseOrderRepository.save(org.mockito.ArgumentMatchers.any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final PurchaseOrder approved = purchaseOrderService.approveStoreRequest(9L, 2L, 20L);

        assertThat(approved.getStatus()).isEqualTo(PurchaseOrderStatus.ACCEPTED);
        assertThat(approved.getRequestingCenter()).isSameAs(center);
        assertThat(approved.getTargetWarehouse()).isSameAs(warehouse);
        verify(scopeGuard).assertCenterWarehouseAccess(2L, 20L);
    }

    @Test
    void cancelRejectsApprovedStoreRequest() {
        final Store store = new Store();
        store.setId(3L);
        final PurchaseOrder po = new PurchaseOrder();
        po.setId(9L);
        po.setRequestingStore(store);
        po.setStatus(PurchaseOrderStatus.ACCEPTED);
        when(purchaseOrderRepository.findById(9L)).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> purchaseOrderService.cancel(9L, "변심"))
                .isInstanceOf(InvalidOperationException.class);
    }
}
