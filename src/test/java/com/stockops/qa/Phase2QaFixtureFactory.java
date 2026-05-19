package com.stockops.qa;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.dto.CreateOutboundRequest;
import com.stockops.entity.Center;
import com.stockops.entity.Location;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.entity.Product;
import com.stockops.entity.Role;
import com.stockops.entity.User;
import com.stockops.entity.Warehouse;
import com.stockops.entity.analytics.AnalyticsExpiryWaste;
import com.stockops.entity.analytics.AnalyticsFillRateSource;
import com.stockops.entity.analytics.AnalyticsPurchaseOrderLeadTime;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.UserRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.repository.analytics.AnalyticsExpiryWasteRepository;
import com.stockops.repository.analytics.AnalyticsFillRateSourceRepository;
import com.stockops.repository.analytics.AnalyticsPurchaseOrderLeadTimeRepository;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAssignment;
import com.stockops.security.ScopeType;
import com.stockops.security.ScopedUserDetails;
import com.stockops.service.InventoryService;
import com.stockops.service.OutboundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds deterministic Phase 2 QA fixtures shared by smoke integration tests.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
public class Phase2QaFixtureFactory {

    private final CenterRepository centerRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final LotRepository lotRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InventoryService inventoryService;
    private final OutboundService outboundService;
    private final AnalyticsExpiryWasteRepository analyticsExpiryWasteRepository;
    private final AnalyticsPurchaseOrderLeadTimeRepository analyticsPurchaseOrderLeadTimeRepository;
    private final AnalyticsFillRateSourceRepository analyticsFillRateSourceRepository;
    private final PasswordEncoder passwordEncoder;

    public Phase2QaFixtureFactory(final CenterRepository centerRepository,
                                  final WarehouseRepository warehouseRepository,
                                  final LocationRepository locationRepository,
                                  final ProductRepository productRepository,
                                  final LotRepository lotRepository,
                                  final UserRepository userRepository,
                                  final RoleRepository roleRepository,
                                  final InventoryService inventoryService,
                                  final OutboundService outboundService,
                                  final AnalyticsExpiryWasteRepository analyticsExpiryWasteRepository,
                                  final AnalyticsPurchaseOrderLeadTimeRepository analyticsPurchaseOrderLeadTimeRepository,
                                  final AnalyticsFillRateSourceRepository analyticsFillRateSourceRepository,
                                  final PasswordEncoder passwordEncoder) {
        this.centerRepository = centerRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.lotRepository = lotRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.inventoryService = inventoryService;
        this.outboundService = outboundService;
        this.analyticsExpiryWasteRepository = analyticsExpiryWasteRepository;
        this.analyticsPurchaseOrderLeadTimeRepository = analyticsPurchaseOrderLeadTimeRepository;
        this.analyticsFillRateSourceRepository = analyticsFillRateSourceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates one scoped Phase 2 operational fixture with analytics-ready history.
     *
     * @return seeded fixture bundle
     */
    public Phase2QaFixture seedPhase2Flow() {
        final Center center = new Center();
        center.setCode("CENTER-A");
        center.setName("Center A");
        final Center savedCenter = centerRepository.save(center);

        final Warehouse primaryWarehouse = new Warehouse();
        primaryWarehouse.setCenter(savedCenter);
        primaryWarehouse.setCode("WH-10");
        primaryWarehouse.setName("Warehouse 10");
        final Warehouse savedPrimaryWarehouse = warehouseRepository.save(primaryWarehouse);

        final Center secondaryCenter = new Center();
        secondaryCenter.setCode("CENTER-B");
        secondaryCenter.setName("Center B");
        final Center savedSecondaryCenter = centerRepository.save(secondaryCenter);

        final Warehouse secondaryWarehouse = new Warehouse();
        secondaryWarehouse.setCenter(savedSecondaryCenter);
        secondaryWarehouse.setCode("WH-11");
        secondaryWarehouse.setName("Warehouse 11");
        final Warehouse savedSecondaryWarehouse = warehouseRepository.save(secondaryWarehouse);

        final Location location = new Location();
        location.setWarehouse(savedPrimaryWarehouse);
        location.setCode("LOC-10");
        location.setName("Primary Storage");
        location.setType("STORAGE");
        final Location savedLocation = locationRepository.save(location);

        final Product product = new Product();
        product.setBarcode("P-1001");
        product.setName("Analytics Product");
        product.setUnit("EA");
        product.setExpiryManaged(true);
        product.setDefaultPrice(BigDecimal.ONE);
        product.setSafetyStockQuantity(12);
        final Product savedProduct = productRepository.save(product);

        final Lot lot = new Lot();
        lot.setLotNumber("LOT-1001");
        lot.setProductId(savedProduct.getId());
        lot.setExpiryDate(LocalDate.of(2026, 12, 31));
        lot.setReceivedDate(LocalDate.of(2026, 4, 1));
        lot.setQuantity(120);
        lot.setStatus(LotStatus.ACTIVE);
        final Lot savedLot = lotRepository.save(lot);

        final Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        final User scopedUser = new User();
        scopedUser.setEmail("manager.center-a@stockops.local");
        scopedUser.setPassword(passwordEncoder.encode("Password123!"));
        scopedUser.setName("Center A Manager");
        scopedUser.setEnabled(true);
        scopedUser.setRole(adminRole);
        scopedUser.setScopeAssignments(new LinkedHashSet<>(Set.of(new ScopeAssignment(ScopeType.WAREHOUSE, savedCenter.getId(), savedPrimaryWarehouse.getId()))));
        final User savedScopedUser = userRepository.save(scopedUser);

        final User systemUser = new User();
        systemUser.setEmail("phase2-system-" + System.nanoTime() + "@stockops.local");
        systemUser.setPassword(passwordEncoder.encode("Password123!"));
        systemUser.setName("Phase2 System User");
        systemUser.setEnabled(true);
        systemUser.setRole(adminRole);
        final User savedSystemUser = userRepository.save(systemUser);
        authenticateAsGlobalAdmin(savedSystemUser);

        inventoryService.increaseStock(savedProduct.getId(), savedLocation.getId(), savedLot.getId(), 120, "INBOUND", 1L, null);

        for (int day = 0; day < 28; day++) {
            final LocalDate outboundDate = LocalDate.of(2026, 5, 1).minusDays(28L - day);
            final var outbound = outboundService.createOutbound(new CreateOutboundRequest(outboundDate, "QA Customer"), savedSystemUser.getId());
            outboundService.addItem(outbound.id(), new AddOutboundItemRequest(savedProduct.getId(), day % 2 == 0 ? 3 : 4));
            outboundService.confirmOutbound(outbound.id(), savedSystemUser.getId());
        }

        final AnalyticsExpiryWaste expiryWaste = new AnalyticsExpiryWaste();
        expiryWaste.setBusinessDate(LocalDate.of(2026, 4, 18));
        expiryWaste.setProductId(savedProduct.getId());
        expiryWaste.setCenterId(savedCenter.getId());
        expiryWaste.setWarehouseId(savedPrimaryWarehouse.getId());
        expiryWaste.setQuarantinedQuantity(5);
        expiryWaste.setQuarantinedLotCount(2);
        analyticsExpiryWasteRepository.save(expiryWaste);

        final AnalyticsPurchaseOrderLeadTime leadTime = new AnalyticsPurchaseOrderLeadTime();
        leadTime.setBusinessDate(LocalDate.of(2026, 4, 18));
        leadTime.setProductId(savedProduct.getId());
        leadTime.setCenterId(savedCenter.getId());
        leadTime.setWarehouseId(savedPrimaryWarehouse.getId());
        leadTime.setPurchaseOrderCount(1);
        leadTime.setLeadTimeSampleCount(1);
        leadTime.setTotalLeadTimeHours(48L);
        analyticsPurchaseOrderLeadTimeRepository.save(leadTime);

        final AnalyticsFillRateSource fillRateSource = new AnalyticsFillRateSource();
        fillRateSource.setBusinessDate(LocalDate.of(2026, 4, 18));
        fillRateSource.setProductId(savedProduct.getId());
        fillRateSource.setCenterId(savedCenter.getId());
        fillRateSource.setWarehouseId(savedPrimaryWarehouse.getId());
        fillRateSource.setPurchaseOrderCount(1);
        fillRateSource.setRequestedQuantity(20);
        fillRateSource.setAcceptedQuantity(16);
        fillRateSource.setCancelledQuantity(4);
        fillRateSource.setShippedQuantity(12);
        analyticsFillRateSourceRepository.save(fillRateSource);

        SecurityContextHolder.clearContext();
        return new Phase2QaFixture(savedCenter, savedPrimaryWarehouse, savedSecondaryWarehouse, savedLocation, savedProduct, savedScopedUser);
    }

    private void authenticateAsGlobalAdmin(final User user) {
        final ScopeAccessProfile scope = new ScopeAccessProfile(
                true,
                List.of(ScopeAssignment.global()),
                Set.of(),
                Set.of());
        final ScopedUserDetails userDetails = new ScopedUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")),
                scope);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()));
    }

    public record Phase2QaFixture(Center center,
                                  Warehouse primaryWarehouse,
                                  Warehouse secondaryWarehouse,
                                  Location location,
                                  Product product,
                                  User scopedUser) {
    }
}
