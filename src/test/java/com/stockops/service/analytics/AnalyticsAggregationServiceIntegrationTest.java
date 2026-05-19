package com.stockops.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.dto.CreateOutboundRequest;
import com.stockops.dto.OutboundDTO;
import com.stockops.entity.Center;
import com.stockops.entity.Inventory;
import com.stockops.entity.Location;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.entity.Outbound;
import com.stockops.entity.OutboundItem;
import com.stockops.entity.Product;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.PurchaseOrderItem;
import com.stockops.entity.PurchaseOrderShipment;
import com.stockops.entity.PurchaseOrderShipmentItem;
import com.stockops.entity.PurchaseOrderStatus;
import com.stockops.entity.User;
import com.stockops.entity.ShipmentStatus;
import com.stockops.entity.Warehouse;
import com.stockops.entity.analytics.AnalyticsDemandHistory;
import com.stockops.entity.analytics.AnalyticsFillRateSource;
import com.stockops.entity.analytics.AnalyticsPurchaseOrderLeadTime;
import com.stockops.entity.analytics.AnalyticsStockPosition;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.OutboundItemRepository;
import com.stockops.repository.OutboundRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.PurchaseOrderItemRepository;
import com.stockops.repository.PurchaseOrderRepository;
import com.stockops.repository.PurchaseOrderShipmentItemRepository;
import com.stockops.repository.PurchaseOrderShipmentRepository;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.UserRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.repository.analytics.AnalyticsDemandHistoryRepository;
import com.stockops.repository.analytics.AnalyticsFillRateSourceRepository;
import com.stockops.repository.analytics.AnalyticsPurchaseOrderLeadTimeRepository;
import com.stockops.repository.analytics.AnalyticsStockPositionRepository;
import com.stockops.service.InventoryService;
import com.stockops.service.OutboundService;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAssignment;
import com.stockops.security.ScopedUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link AnalyticsAggregationService}.
 *
 * @author StockOps Team
 * @since 2.0
 */
@SpringBootTest
@Transactional
class AnalyticsAggregationServiceIntegrationTest {

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @Autowired
    private AnalyticsDemandHistoryRepository analyticsDemandHistoryRepository;

    @Autowired
    private AnalyticsStockPositionRepository analyticsStockPositionRepository;

    @Autowired
    private AnalyticsPurchaseOrderLeadTimeRepository analyticsPurchaseOrderLeadTimeRepository;

    @Autowired
    private AnalyticsFillRateSourceRepository analyticsFillRateSourceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OutboundService outboundService;

    @Autowired
    private OutboundRepository outboundRepository;

    @Autowired
    private OutboundItemRepository outboundItemRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private PurchaseOrderShipmentRepository purchaseOrderShipmentRepository;

    @Autowired
    private PurchaseOrderShipmentItemRepository purchaseOrderShipmentItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    private Long currentUserId;

    @BeforeEach
    void setUpAuthenticatedUser() {
        final String email = "analytics-test-" + System.nanoTime() + "@stockops.com";
        User user = new User();
        user.setEmail(email);
        user.setPassword("{noop}password");
        user.setName("Analytics Test User");
        user.setRole(roleRepository.findByName("ADMIN").orElseThrow());
        currentUserId = userRepository.save(user).getId();
        final ScopeAccessProfile scope = new ScopeAccessProfile(
                true, List.of(ScopeAssignment.global()), Set.of(), Set.of());
        final ScopedUserDetails userDetails = new ScopedUserDetails(
                currentUserId,
                email,
                "password",
                true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")),
                scope);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));
    }

    /**
     * Verifies that only confirmed outbound activity contributes to demand history and that stock position stays deterministic.
     */
    @Test
    void refreshRangeUsesConfirmedOutboundsOnlyForDemand() {
        final SeedContext seed = seedOperationalContext(1001L, "P-1001", "Confirmed Demand Product");
        inventoryService.increaseStock(seed.product().getId(), seed.location().getId(), seed.lot().getId(), 40, "INBOUND", 1L, null);

        createDraftOutbound(seed.product().getId(), LocalDate.of(2026, 4, 20), 5);
        createConfirmedOutbound(seed.product().getId(), LocalDate.of(2026, 4, 20), 7);
        createConfirmedOutbound(seed.product().getId(), LocalDate.of(2026, 4, 20), 3);

        entityManager.flush();
        analyticsAggregationService.refreshRange(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 20));

        final AnalyticsDemandHistory demand = analyticsDemandHistoryRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        LocalDate.of(2026, 4, 20), seed.product().getId(), seed.center().getId(), seed.warehouse().getId())
                .orElseThrow();
        assertThat(demand.getConfirmedOutboundQuantity()).isEqualTo(10);
        assertThat(demand.getConfirmedOutboundEventCount()).isEqualTo(2);
        assertThat(demand.isInsufficientHistory()).isFalse();

        final AnalyticsStockPosition stockPosition = analyticsStockPositionRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        LocalDate.of(2026, 4, 20), seed.product().getId(), seed.center().getId(), seed.warehouse().getId())
                .orElseThrow();
        assertThat(stockPosition.getOnHandQuantity()).isZero();
        assertThat(stockPosition.getAvailableQuantity()).isZero();
    }

    /**
     * Verifies that missing confirmed demand history stays at zero even if cancelled outbound rows exist.
     */
    @Test
    void refreshRangeMarksCancelledOrEmptyHistoryAsInsufficientDemand() {
        final SeedContext seed = seedOperationalContext(2002L, "P-2002", "No Confirmed Demand Product");
        inventoryService.increaseStock(seed.product().getId(), seed.location().getId(), seed.lot().getId(), 15, "INBOUND", 2L, null);

        final Outbound cancelledOutbound = new Outbound();
        cancelledOutbound.setOutboundDate(LocalDate.of(2026, 4, 21));
        cancelledOutbound.setCustomer("Cancelled Customer");
        cancelledOutbound.setStatus("CANCELLED");
        cancelledOutbound.setTotalQuantity(4);
        final Outbound savedOutbound = outboundRepository.save(cancelledOutbound);

        final OutboundItem cancelledItem = new OutboundItem();
        cancelledItem.setOutboundId(savedOutbound.getId());
        cancelledItem.setProductId(seed.product().getId());
        cancelledItem.setQuantity(4);
        outboundItemRepository.save(cancelledItem);

        entityManager.flush();
        analyticsAggregationService.refreshRange(LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21));

        final AnalyticsDemandHistory demand = analyticsDemandHistoryRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        LocalDate.of(2026, 4, 21), seed.product().getId(), seed.center().getId(), seed.warehouse().getId())
                .orElseThrow();
        assertThat(demand.getConfirmedOutboundQuantity()).isZero();
        assertThat(demand.getConfirmedOutboundEventCount()).isZero();
        assertThat(demand.isInsufficientHistory()).isTrue();
    }

    /**
     * Verifies purchase-order lead-time and fill-rate source aggregation for downstream analytics.
     */
    @Test
    void refreshRangeAggregatesPurchaseOrderLeadTimeAndFillRateSource() {
        final SeedContext seed = seedOperationalContext(3003L, "P-3003", "Purchase Order Analytics Product");

        final PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNumber("PO-20260418-001");
        purchaseOrder.setRequestingCenter(seed.center());
        purchaseOrder.setTargetWarehouse(seed.warehouse());
        purchaseOrder.setStatus(PurchaseOrderStatus.ACCEPTED);
        purchaseOrder.setRequestedAt(LocalDateTime.of(2026, 4, 18, 9, 0));
        purchaseOrder.setErpRespondedAt(LocalDateTime.of(2026, 4, 20, 9, 0));
        final PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        final PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
        purchaseOrderItem.setPurchaseOrder(savedPurchaseOrder);
        purchaseOrderItem.setProduct(seed.product());
        purchaseOrderItem.setRequestedQuantity(20);
        purchaseOrderItem.setAcceptedQuantity(16);
        purchaseOrderItem.setCancelledQuantity(4);
        purchaseOrderItem.setUnitPrice(BigDecimal.TEN);
        purchaseOrderItem.setTotalPrice(BigDecimal.valueOf(200));
        final PurchaseOrderItem savedItem = purchaseOrderItemRepository.save(purchaseOrderItem);

        final PurchaseOrderShipment shipment = new PurchaseOrderShipment();
        shipment.setPurchaseOrder(savedPurchaseOrder);
        shipment.setShipmentNumber("SHIP-001");
        shipment.setStatus(ShipmentStatus.CREATED);
        final PurchaseOrderShipment savedShipment = purchaseOrderShipmentRepository.save(shipment);

        final PurchaseOrderShipmentItem shipmentItem = new PurchaseOrderShipmentItem();
        shipmentItem.setShipment(savedShipment);
        shipmentItem.setPurchaseOrderItem(savedItem);
        shipmentItem.setShippedQuantity(12);
        purchaseOrderShipmentItemRepository.save(shipmentItem);

        entityManager.flush();
        analyticsAggregationService.refreshRange(LocalDate.of(2026, 4, 18), LocalDate.of(2026, 4, 18));

        final AnalyticsPurchaseOrderLeadTime leadTime = analyticsPurchaseOrderLeadTimeRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        LocalDate.of(2026, 4, 18), seed.product().getId(), seed.center().getId(), seed.warehouse().getId())
                .orElseThrow();
        assertThat(leadTime.getPurchaseOrderCount()).isEqualTo(1);
        assertThat(leadTime.getLeadTimeSampleCount()).isEqualTo(1);
        assertThat(leadTime.getTotalLeadTimeHours()).isEqualTo(48);

        final AnalyticsFillRateSource fillRate = analyticsFillRateSourceRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        LocalDate.of(2026, 4, 18), seed.product().getId(), seed.center().getId(), seed.warehouse().getId())
                .orElseThrow();
        assertThat(fillRate.getPurchaseOrderCount()).isEqualTo(1);
        assertThat(fillRate.getRequestedQuantity()).isEqualTo(20);
        assertThat(fillRate.getAcceptedQuantity()).isEqualTo(16);
        assertThat(fillRate.getCancelledQuantity()).isEqualTo(4);
        assertThat(fillRate.getShippedQuantity()).isEqualTo(12);
    }

    private SeedContext seedOperationalContext(final Long productNumericSuffix,
                                               final String barcode,
                                               final String productName) {
        final Center center = new Center();
        center.setCode("CENTER-" + productNumericSuffix);
        center.setName("Center " + productNumericSuffix);
        final Center savedCenter = centerRepository.save(center);

        final Warehouse warehouse = new Warehouse();
        warehouse.setCenter(savedCenter);
        warehouse.setCode("WH-" + productNumericSuffix);
        warehouse.setName("Warehouse " + productNumericSuffix);
        final Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        final Location location = new Location();
        location.setWarehouse(savedWarehouse);
        location.setCode("LOC-" + productNumericSuffix);
        location.setName("Location " + productNumericSuffix);
        location.setType("STORAGE");
        final Location savedLocation = locationRepository.save(location);

        final Product product = new Product();
        product.setBarcode(barcode);
        product.setName(productName);
        product.setUnit("EA");
        product.setExpiryManaged(true);
        product.setDefaultPrice(BigDecimal.ONE);
        final Product savedProduct = productRepository.save(product);

        final Lot lot = new Lot();
        lot.setLotNumber("LOT-" + productNumericSuffix);
        lot.setProductId(savedProduct.getId());
        lot.setExpiryDate(LocalDate.of(2026, 12, 31));
        lot.setReceivedDate(LocalDate.of(2026, 4, 1));
        lot.setQuantity(100);
        lot.setStatus(LotStatus.ACTIVE);
        final Lot savedLot = lotRepository.save(lot);

        return new SeedContext(savedCenter, savedWarehouse, savedLocation, savedProduct, savedLot);
    }

    private void createDraftOutbound(final Long productId, final LocalDate outboundDate, final int quantity) {
        final OutboundDTO outbound = outboundService.createOutbound(
                new CreateOutboundRequest(outboundDate, "Draft Customer"), currentUserId);
        outboundService.addItem(outbound.id(), new AddOutboundItemRequest(productId, quantity));
    }

    private void createConfirmedOutbound(final Long productId, final LocalDate outboundDate, final int quantity) {
        final OutboundDTO outbound = outboundService.createOutbound(
                new CreateOutboundRequest(outboundDate, "Confirmed Customer"), currentUserId);
        outboundService.addItem(outbound.id(), new AddOutboundItemRequest(productId, quantity));
        outboundService.confirmOutbound(outbound.id(), currentUserId);
    }

    private record SeedContext(Center center, Warehouse warehouse, Location location, Product product, Lot lot) {
    }
}
