package com.stockops.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.dto.AIRecommendationDTO;
import com.stockops.dto.CreateOutboundRequest;
import com.stockops.dto.OutboundDTO;
import com.stockops.entity.Center;
import com.stockops.entity.Location;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.entity.Product;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.PurchaseOrderStatus;
import com.stockops.entity.User;
import com.stockops.entity.Warehouse;
import com.stockops.entity.ai.AIForecastSnapshot;
import com.stockops.entity.ai.AIRecommendation;
import com.stockops.entity.ai.AIRecommendationStatus;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.PurchaseOrderRepository;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.UserRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.repository.ai.AIForecastSnapshotRepository;
import com.stockops.repository.ai.AIRecommendationRepository;
import com.stockops.service.InventoryService;
import com.stockops.service.OutboundService;
import com.stockops.service.analytics.AnalyticsAggregationService;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAssignment;
import com.stockops.security.ScopedUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link AIRecommendationService}.
 *
 * @author StockOps Team
 * @since 2.0
 */
@SpringBootTest
@Transactional
class AIRecommendationServiceIntegrationTest {

    private static final LocalDate RECOMMENDATION_DATE = LocalDate.of(2026, 5, 1);

    @Autowired
    private AIRecommendationService aiRecommendationService;

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @Autowired
    private AIRecommendationRepository aiRecommendationRepository;

    @Autowired
    private AIForecastSnapshotRepository aiForecastSnapshotRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long currentUserId;

    @BeforeEach
    void setUpAuthenticatedUser() {
        final User admin = userRepository.findByEmail("admin@stockops.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail("admin@stockops.com");
                    user.setPassword("{noop}password");
                    user.setName("Admin User");
                    user.setRole(roleRepository.findByName("ADMIN").orElseThrow());
                    return userRepository.save(user);
                });
        currentUserId = admin.getId();
        final ScopeAccessProfile scope = new ScopeAccessProfile(
                true, List.of(ScopeAssignment.global()), Set.of(), Set.of());
        final ScopedUserDetails userDetails = new ScopedUserDetails(
                currentUserId,
                admin.getEmail(),
                "password",
                true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")),
                scope);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));
    }

    /**
     * Verifies that the recommendation batch persists one deterministic ready-for-approval snapshot per scope.
     */
    @Test
    void generateRecommendationsCreatesReadyRecommendationAndRemainsIdempotent() {
        final SeedContext seed = seedOperationalContext(1001L, "P-1001", "Forecasted Product", 12, 120);

        for (int day = 0; day < 28; day++) {
            final LocalDate demandDate = RECOMMENDATION_DATE.minusDays(28L - day);
            createConfirmedOutbound(seed.product().getId(), demandDate, day % 2 == 0 ? 3 : 4);
        }

        refreshAnalyticsForRecommendationDate();
        aiRecommendationService.generateRecommendationsForBusinessDate(RECOMMENDATION_DATE);
        aiRecommendationService.generateRecommendationsForBusinessDate(RECOMMENDATION_DATE);

        final AIRecommendation recommendation = aiRecommendationRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        RECOMMENDATION_DATE,
                        seed.product().getId(),
                        seed.center().getId(),
                        seed.warehouse().getId())
                .orElseThrow();
        final AIForecastSnapshot forecastSnapshot = aiForecastSnapshotRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        RECOMMENDATION_DATE,
                        seed.product().getId(),
                        seed.center().getId(),
                        seed.warehouse().getId())
                .orElseThrow();

        assertThat(recommendation.getStatus()).isEqualTo(AIRecommendationStatus.READY_FOR_APPROVAL);
        assertThat(recommendation.getRecommendedQuantity()).isPositive();
        assertThat(forecastSnapshot.getSevenDayForecastQuantity()).isPositive();
        assertThat(aiRecommendationRepository.findByBusinessDate(RECOMMENDATION_DATE)).hasSize(1);
        assertThat(aiForecastSnapshotRepository.findByBusinessDate(RECOMMENDATION_DATE)).hasSize(1);

        final AIRecommendationDTO dto = aiRecommendationService.listRecommendations(
                RECOMMENDATION_DATE,
                seed.center().getId(),
                seed.warehouse().getId(),
                seed.product().getId()).getFirst();
        assertThat(dto.status()).isEqualTo(AIRecommendationStatus.READY_FOR_APPROVAL);
        assertThat(dto.recommendedQuantity()).isPositive();
    }

    /**
     * Verifies that a stocked product with no confirmed outbound history is marked as insufficient history.
     */
    @Test
    void generateRecommendationsMarksColdStartProductAsInsufficientHistory() {
        final SeedContext seed = seedOperationalContext(2002L, "P-2002", "Cold Start Product", 7, 5);

        refreshAnalyticsForRecommendationDate();
        aiRecommendationService.generateRecommendationsForBusinessDate(RECOMMENDATION_DATE);

        final AIRecommendation recommendation = aiRecommendationRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        RECOMMENDATION_DATE,
                        seed.product().getId(),
                        seed.center().getId(),
                        seed.warehouse().getId())
                .orElseThrow();

        assertThat(recommendation.getStatus()).isEqualTo(AIRecommendationStatus.INSUFFICIENT_HISTORY);
        assertThat(recommendation.getRecommendedQuantity()).isZero();
        assertThat(recommendation.getApprovedPurchaseOrder()).isNull();
    }

    /**
     * Verifies that approval creates a draft purchase order and never auto-submits it.
     */
    @Test
    void approveRecommendationCreatesDraftPurchaseOrder() {
        final SeedContext seed = seedOperationalContext(3003L, "P-3003", "Approval Product", 10, 90);

        for (int day = 0; day < 28; day++) {
            final LocalDate demandDate = RECOMMENDATION_DATE.minusDays(28L - day);
            createConfirmedOutbound(seed.product().getId(), demandDate, 3);
        }

        refreshAnalyticsForRecommendationDate();
        aiRecommendationService.generateRecommendationsForBusinessDate(RECOMMENDATION_DATE);

        final AIRecommendation recommendation = aiRecommendationRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseId(
                        RECOMMENDATION_DATE,
                        seed.product().getId(),
                        seed.center().getId(),
                        seed.warehouse().getId())
                .orElseThrow();
        final User approvingUser = userRepository.findByEmail("admin@stockops.com").orElseThrow();

        final AIRecommendationDTO approvedRecommendation = aiRecommendationService.approveRecommendation(
                recommendation.getId(),
                approvingUser);

        final PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findById(approvedRecommendation.approvedPurchaseOrderId())
                .orElseThrow();

        assertThat(approvedRecommendation.status()).isEqualTo(AIRecommendationStatus.APPROVED_TO_DRAFT);
        assertThat(approvedRecommendation.approvedPurchaseOrderId()).isNotNull();
        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(purchaseOrder.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProduct().getId()).isEqualTo(seed.product().getId());
            assertThat(item.getRequestedQuantity()).isEqualTo(approvedRecommendation.recommendedQuantity());
        });
    }

    private SeedContext seedOperationalContext(final Long productNumericSuffix,
                                               final String barcode,
                                               final String productName,
                                               final int safetyStockQuantity,
                                               final int initialStockQuantity) {
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
        product.setSafetyStockQuantity(safetyStockQuantity);
        final Product savedProduct = productRepository.save(product);

        final Lot lot = new Lot();
        lot.setLotNumber("LOT-" + productNumericSuffix);
        lot.setProductId(savedProduct.getId());
        lot.setExpiryDate(LocalDate.of(2026, 12, 31));
        lot.setReceivedDate(LocalDate.of(2026, 4, 1));
        lot.setQuantity(initialStockQuantity);
        lot.setStatus(LotStatus.ACTIVE);
        final Lot savedLot = lotRepository.save(lot);

        inventoryService.increaseStock(savedProduct.getId(), savedLocation.getId(), savedLot.getId(), initialStockQuantity, "INBOUND", 1L, null);

        return new SeedContext(savedCenter, savedWarehouse, savedLocation, savedProduct, savedLot);
    }

    private void createConfirmedOutbound(final Long productId, final LocalDate outboundDate, final int quantity) {
        final OutboundDTO outbound = outboundService.createOutbound(
                new CreateOutboundRequest(outboundDate, "AI Demand Customer"), currentUserId);
        outboundService.addItem(outbound.id(), new AddOutboundItemRequest(productId, quantity));
        outboundService.confirmOutbound(outbound.id(), currentUserId);
    }

    private void refreshAnalyticsForRecommendationDate() {
        final LocalDate today = LocalDate.now();
        final LocalDate historyStart = RECOMMENDATION_DATE.minusDays(28);
        final LocalDate firstRefreshEnd = RECOMMENDATION_DATE.isBefore(today) ? RECOMMENDATION_DATE : today;
        analyticsAggregationService.refreshRange(historyStart, firstRefreshEnd);
        if (RECOMMENDATION_DATE.isAfter(today)) {
            analyticsAggregationService.refreshRange(today.plusDays(1), RECOMMENDATION_DATE);
        }
    }

    private record SeedContext(Center center, Warehouse warehouse, Location location, Product product, Lot lot) {
    }
}
