package com.stockops.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockops.dto.AddOutboundItemRequest;
import com.stockops.dto.CreateOutboundRequest;
import com.stockops.dto.OutboundDTO;
import com.stockops.entity.Center;
import com.stockops.entity.Location;
import com.stockops.entity.Lot;
import com.stockops.entity.LotStatus;
import com.stockops.entity.Product;
import com.stockops.entity.User;
import com.stockops.entity.Warehouse;
import com.stockops.entity.ai.ForecastProposalRun;
import com.stockops.entity.ai.ForecastProposalStatus;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.LotRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.UserRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.repository.ai.ForecastProposalRunRepository;
import com.stockops.security.ScopeAccessProfile;
import com.stockops.security.ScopeAssignment;
import com.stockops.security.ScopedUserDetails;
import com.stockops.service.InventoryService;
import com.stockops.service.OutboundService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
 * Integration tests for {@link IntradayForecastService} — proves the core mechanism: live stock
 * depletion drives intraday variation, and each slot accumulates its own proposal row.
 *
 * @author StockOps Team
 * @since 2.4
 */
@SpringBootTest
@Transactional
class IntradayForecastServiceIntegrationTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 5, 1);
    private static final Instant MORNING_RUN = Instant.parse("2026-05-01T01:00:00Z");
    private static final Instant MIDDAY_RUN = Instant.parse("2026-05-01T06:00:00Z");

    @Autowired
    private IntradayForecastService intradayForecastService;

    @Autowired
    private ForecastProposalRunRepository proposalRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OutboundService outboundService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long currentUserId;

    @BeforeEach
    void setUpAuthenticatedUser() {
        final User admin = userRepository.findByEmail("admin@stockops.com")
                .orElseGet(() -> {
                    final User user = new User();
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
                currentUserId, admin.getEmail(), "password", true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")), scope);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void slotsAccumulateAndLaterSlotReactsToLiveStockDepletion() {
        // High safety stock so the proposal stays positive; ample initial stock to ship from.
        final SeedContext seed = seedOperationalContext(9001L, "P-9001", "Intraday Product", 250, 200);

        // 28 settled days of confirmed demand (3/day) build the forecast; same-day demand is ignored
        // by the model, so the only intraday signal is current stock.
        for (int day = 0; day < 28; day++) {
            createConfirmedOutbound(seed.product().getId(), BUSINESS_DATE.minusDays(28L - day), 3);
        }

        // The test shares one transaction with the service's raw-SQL stock query; flush so the JPA
        // inventory writes are visible to that query (in production each slot is its own committed tx).
        entityManager.flush();
        intradayForecastService.generateProposalsForSlot(BUSINESS_DATE, 10, MORNING_RUN);
        final ForecastProposalRun morning = proposal(seed, 10);

        // Deplete live stock by 5 between slots, then run the 15:00 slot.
        createConfirmedOutbound(seed.product().getId(), BUSINESS_DATE, 5);
        entityManager.flush();
        intradayForecastService.generateProposalsForSlot(BUSINESS_DATE, 15, MIDDAY_RUN);
        final ForecastProposalRun midday = proposal(seed, 15);

        // Accumulation: both slots persisted as separate rows.
        assertThat(proposalRepository.findByBusinessDateOrderByRunSlotDescRecommendedQuantityDescIdAsc(BUSINESS_DATE))
                .hasSize(2);

        // Intraday variation: 15:00 saw 5 fewer units of stock, so it recommends exactly 5 more.
        assertThat(morning.getStatus()).isEqualTo(ForecastProposalStatus.PROPOSED);
        assertThat(midday.getStatus()).isEqualTo(ForecastProposalStatus.PROPOSED);
        assertThat(midday.getCurrentStockQuantity()).isEqualTo(morning.getCurrentStockQuantity() - 5);
        assertThat(midday.getRecommendedQuantity()).isEqualTo(morning.getRecommendedQuantity() + 5);

        // 3-day actionable window anchored to each run instant.
        assertThat(morning.getActionableUntil()).isEqualTo(MORNING_RUN.plus(Duration.ofDays(3)));
        assertThat(midday.getActionableUntil()).isEqualTo(MIDDAY_RUN.plus(Duration.ofDays(3)));
    }

    @Test
    void approvedScopeIsNotReProposedInLaterSlot() {
        final SeedContext seed = seedOperationalContext(9002L, "P-9002", "Approve-Then-Slot Product", 250, 200);
        for (int day = 0; day < 28; day++) {
            createConfirmedOutbound(seed.product().getId(), BUSINESS_DATE.minusDays(28L - day), 3);
        }
        entityManager.flush();

        // 10:00 slot runs and the proposal is approved (current-time run so it is within the window).
        final Instant morningRun = Instant.now();
        intradayForecastService.generateProposalsForSlot(BUSINESS_DATE, 10, morningRun);
        final ForecastProposalRun morning = proposal(seed, 10);
        final User approver = userRepository.findByEmail("admin@stockops.com").orElseThrow();
        intradayForecastService.approveProposal(morning.getId(), approver);
        entityManager.flush();

        // 15:00 slot runs afterward — the already-approved scope must NOT get a new approvable proposal.
        intradayForecastService.generateProposalsForSlot(BUSINESS_DATE, 15, Instant.now());

        assertThat(proposalRepository.findByBusinessDateAndRunSlotAndProductIdAndCenterIdAndWarehouseId(
                BUSINESS_DATE, 15, seed.product().getId(), seed.center().getId(), seed.warehouse().getId()))
                .isEmpty();
        assertThat(proposal(seed, 10).getStatus()).isEqualTo(ForecastProposalStatus.APPROVED_TO_DRAFT);
    }

    @Test
    void sweepExpiresPastWindowProposals() {
        final SeedContext seed = seedOperationalContext(9003L, "P-9003", "Expiry Sweep Product", 250, 200);
        for (int day = 0; day < 28; day++) {
            createConfirmedOutbound(seed.product().getId(), BUSINESS_DATE.minusDays(28L - day), 3);
        }
        entityManager.flush();

        // MORNING_RUN (2026-05-01) → actionable_until 2026-05-04, already past the current clock.
        intradayForecastService.generateProposalsForSlot(BUSINESS_DATE, 10, MORNING_RUN);
        assertThat(proposal(seed, 10).getStatus()).isEqualTo(ForecastProposalStatus.PROPOSED);

        final int expired = intradayForecastService.expireStaleProposals();

        assertThat(expired).isEqualTo(1);
        assertThat(proposal(seed, 10).getStatus()).isEqualTo(ForecastProposalStatus.EXPIRED);
    }

    private ForecastProposalRun proposal(final SeedContext seed, final int runSlot) {
        return proposalRepository.findByBusinessDateAndRunSlotAndProductIdAndCenterIdAndWarehouseId(
                BUSINESS_DATE, runSlot, seed.product().getId(), seed.center().getId(), seed.warehouse().getId())
                .orElseThrow();
    }

    private SeedContext seedOperationalContext(final Long suffix,
                                               final String barcode,
                                               final String productName,
                                               final int safetyStockQuantity,
                                               final int initialStockQuantity) {
        final Center center = new Center();
        center.setCode("CENTER-" + suffix);
        center.setName("Center " + suffix);
        final Center savedCenter = centerRepository.save(center);

        final Warehouse warehouse = new Warehouse();
        warehouse.setCenter(savedCenter);
        warehouse.setCode("WH-" + suffix);
        warehouse.setName("Warehouse " + suffix);
        final Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        final Location location = new Location();
        location.setWarehouse(savedWarehouse);
        location.setCode("LOC-" + suffix);
        location.setName("Location " + suffix);
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
        lot.setLotNumber("LOT-" + suffix);
        lot.setProductId(savedProduct.getId());
        lot.setExpiryDate(LocalDate.of(2026, 12, 31));
        lot.setReceivedDate(LocalDate.of(2026, 4, 1));
        lot.setQuantity(initialStockQuantity);
        lot.setStatus(LotStatus.ACTIVE);
        final Lot savedLot = lotRepository.save(lot);

        inventoryService.increaseStock(
                savedProduct.getId(), savedLocation.getId(), savedLot.getId(), initialStockQuantity, "INBOUND", 1L, null);

        return new SeedContext(savedCenter, savedWarehouse, savedLocation, savedProduct, savedLot);
    }

    private void createConfirmedOutbound(final Long productId, final LocalDate outboundDate, final int quantity) {
        final OutboundDTO outbound = outboundService.createOutbound(
                new CreateOutboundRequest(outboundDate, "Intraday Demand Customer"), currentUserId);
        outboundService.addItem(outbound.id(), new AddOutboundItemRequest(productId, quantity));
        outboundService.confirmOutbound(outbound.id(), currentUserId);
    }

    private record SeedContext(Center center, Warehouse warehouse, Location location, Product product, Lot lot) {
    }
}
