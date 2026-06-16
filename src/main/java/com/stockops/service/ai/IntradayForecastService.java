package com.stockops.service.ai;

import com.stockops.ai.forecast.ForecastContext;
import com.stockops.ai.forecast.ForecastContext.DemandDataPoint;
import com.stockops.ai.forecast.ForecastContext.ForecastParameters;
import com.stockops.ai.forecast.ForecastContext.LeadTimeInfo;
import com.stockops.ai.forecast.ForecastModel;
import com.stockops.ai.forecast.ForecastResult;
import com.stockops.dto.ForecastProposalRunDTO;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.Product;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.User;
import com.stockops.entity.ai.AIRecommendationStatus;
import com.stockops.entity.ai.ForecastProposalRun;
import com.stockops.entity.ai.ForecastProposalStatus;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.ai.ForecastProposalRunRepository;
import com.stockops.security.ScopeGuard;
import com.stockops.service.PurchaseOrderService;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds intraday reorder proposals from LIVE transactional data and accumulates one proposal row
 * per scheduled slot.
 *
 * <p>The daily {@link AIRecommendationService} reads the once-a-day {@code analytics.daily_*} read
 * model (refreshed at 01:15, same-day demand excluded), so it cannot vary within a day. This service
 * instead reads current stock straight from {@code inventory} (kept live by every inbound/outbound
 * transaction) and recent demand from {@code inventory_transactions}/{@code outbounds}. Because the
 * statistical model's reorder quantity is driven by current stock, consuming stock through the day
 * makes the 10:00 and 15:00 proposals genuinely differ.
 *
 * @author StockOps Team
 * @since 2.4
 */
@Service
public class IntradayForecastService {

    private static final int FORECAST_HORIZON_DAYS = 7;
    private static final BigDecimal TRAILING_WEIGHT = new BigDecimal("0.70");
    private static final BigDecimal WEEKDAY_WEIGHT = new BigDecimal("0.30");

    private static final Logger log = LoggerFactory.getLogger(IntradayForecastService.class);

    private final IntradayForecastProperties properties;
    private final AIRecommendationProperties forecastTuning;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final ForecastProposalRunRepository proposalRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final ScopeGuard scopeGuard;
    private final ForecastModel defaultForecastModel;
    private final Map<String, ForecastModel> forecastModels;

    public IntradayForecastService(
            final IntradayForecastProperties properties,
            final AIRecommendationProperties forecastTuning,
            final NamedParameterJdbcTemplate jdbcTemplate,
            final ProductRepository productRepository,
            final ForecastProposalRunRepository proposalRepository,
            final PurchaseOrderService purchaseOrderService,
            final ScopeGuard scopeGuard,
            @Qualifier("statisticalForecastModel") final ForecastModel defaultForecastModel,
            final Map<String, ForecastModel> forecastModels) {
        this.properties = properties;
        this.forecastTuning = forecastTuning;
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.proposalRepository = proposalRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.scopeGuard = scopeGuard;
        this.defaultForecastModel = defaultForecastModel;
        this.forecastModels = forecastModels;
    }

    /**
     * Generates (and refreshes in place) intraday proposals for one slot of a business date.
     * Only scopes whose forecast yields a positive reorder quantity become proposals. Proposals that
     * have already been approved or rejected in this slot are preserved; previously open proposals for
     * the slot that no longer warrant action are removed.
     *
     * @param businessDate business date the slot belongs to
     * @param runSlot scheduled hour-of-day in the business zone (e.g. 10, 15)
     * @param runAt instant the run started
     * @return the fresh open proposals produced by this run
     */
    @Transactional
    @Observed(name = "ai.intraday.generate", contextualName = "generate-intraday-proposals")
    public List<ForecastProposalRun> generateProposalsForSlot(final LocalDate businessDate,
                                                              final int runSlot,
                                                              final Instant runAt) {
        final ForecastModel forecastModel = resolveForecastModel(properties.getForecastModel());
        final Instant actionableUntil = runAt.plus(Duration.ofDays(Math.max(properties.getActionableDays(), 0)));

        final Map<DimensionKey, Integer> liveStock = loadLiveAvailableStock();
        final Map<DimensionKey, List<DemandDataPoint>> demandHistory = loadLiveDemandHistory(
                businessDate.minusDays(forecastTuning.getForecastHistoryDays()), businessDate);
        final Map<DimensionKey, LeadTimeStats> leadTimes = loadLeadTimeStats(
                businessDate.minusDays(forecastTuning.getLeadTimeLookbackDays()), businessDate.minusDays(1));

        final Set<DimensionKey> dimensionKeys = new HashSet<>();
        dimensionKeys.addAll(liveStock.keySet());
        dimensionKeys.addAll(demandHistory.keySet());
        final Map<Long, Product> products = loadProducts(
                dimensionKeys.stream().map(DimensionKey::productId).collect(Collectors.toSet()));

        final Map<DimensionKey, ForecastProposalRun> existingBySlot = indexBySlot(
                proposalRepository.findByBusinessDateAndRunSlot(businessDate, runSlot));

        // A scope already approved (in any slot today) must not get a fresh approvable proposal,
        // otherwise approving a later slot would create a second draft PO for the same scope/day.
        final Set<DimensionKey> approvedScopes = proposalRepository
                .findByBusinessDateAndStatus(businessDate, ForecastProposalStatus.APPROVED_TO_DRAFT).stream()
                .map(p -> new DimensionKey(p.getProductId(), p.getCenterId(), p.getWarehouseId()))
                .collect(Collectors.toSet());

        final List<ForecastProposalRun> freshProposals = new ArrayList<>();
        final Set<DimensionKey> proposedKeys = new HashSet<>();

        for (final DimensionKey key : dimensionKeys) {
            final Product product = products.get(key.productId());
            if (product == null || approvedScopes.contains(key)) {
                continue;
            }
            final ForecastProposalRun existing = existingBySlot.get(key);
            if (existing != null && existing.getStatus() != ForecastProposalStatus.PROPOSED) {
                // Preserve an already approved/rejected proposal for this slot.
                proposedKeys.add(key);
                continue;
            }

            final ForecastContext context = buildContext(key, businessDate, product, liveStock, demandHistory, leadTimes);
            final ForecastResult computation = forecastModel.computeForecast(context);
            if (computation.status() != AIRecommendationStatus.READY_FOR_APPROVAL) {
                continue;
            }

            final ForecastProposalRun proposal = existing == null ? new ForecastProposalRun() : existing;
            applyComputation(proposal, key, businessDate, runSlot, runAt, actionableUntil, context, computation);
            freshProposals.add(proposalRepository.save(proposal));
            proposedKeys.add(key);
        }

        deleteStaleOpenProposals(existingBySlot, proposedKeys);

        log.info("[AI] Intraday slot {} for {} produced {} open proposals using model {}",
                runSlot, businessDate, freshProposals.size(), forecastModel.getModelId());
        return freshProposals;
    }

    /**
     * Approves an open, in-window proposal into a draft purchase order (mirrors the daily approve flow).
     *
     * @param proposalId proposal identifier
     * @param currentUser approving user
     * @return approved proposal payload including the linked draft purchase order
     */
    @Transactional
    public ForecastProposalRunDTO approveProposal(final Long proposalId, final User currentUser) {
        final ForecastProposalRun proposal = loadActionable(proposalId);
        if (proposal.getRecommendedQuantity() == null || proposal.getRecommendedQuantity() <= 0) {
            throw new InvalidOperationException("Proposal quantity must be positive to create a draft purchase order");
        }

        PurchaseOrder purchaseOrder = purchaseOrderService.create(
                proposal.getCenterId(), proposal.getWarehouseId(), currentUser);
        purchaseOrder = purchaseOrderService.addItem(
                purchaseOrder.getId(), proposal.getProductId(), proposal.getRecommendedQuantity());

        proposal.setApprovedPurchaseOrder(purchaseOrder);
        proposal.setApprovedBy(currentUser);
        proposal.setApprovedAt(Instant.now());
        proposal.setStatus(ForecastProposalStatus.APPROVED_TO_DRAFT);
        proposal.setExplanationSummary(appendExplanation(
                proposal.getExplanationSummary(), "Approved into draft purchase order " + purchaseOrder.getPoNumber()));

        final ForecastProposalRun saved = proposalRepository.save(proposal);
        supersedeSiblingProposals(saved);
        return toDTO(saved, loadProducts(Set.of(saved.getProductId())));
    }

    /**
     * Expires other open proposals for the same product/center/warehouse on the same business date,
     * so approving one slot never lets a second approval create a duplicate draft purchase order.
     *
     * @param approved the just-approved proposal
     */
    private void supersedeSiblingProposals(final ForecastProposalRun approved) {
        final List<ForecastProposalRun> siblings = proposalRepository
                .findByBusinessDateAndProductIdAndCenterIdAndWarehouseIdAndStatus(
                        approved.getBusinessDate(),
                        approved.getProductId(),
                        approved.getCenterId(),
                        approved.getWarehouseId(),
                        ForecastProposalStatus.PROPOSED);
        for (final ForecastProposalRun sibling : siblings) {
            if (sibling.getId().equals(approved.getId())) {
                continue;
            }
            sibling.setStatus(ForecastProposalStatus.EXPIRED);
            sibling.setExplanationSummary(appendExplanation(
                    sibling.getExplanationSummary(),
                    "Superseded by approval of slot " + approved.getRunSlot()));
            proposalRepository.save(sibling);
        }
    }

    /**
     * Rejects an open, in-window proposal.
     *
     * @param proposalId proposal identifier
     * @param currentUser rejecting user
     * @param reason optional rejection reason
     * @return rejected proposal payload
     */
    @Transactional
    public ForecastProposalRunDTO rejectProposal(final Long proposalId, final User currentUser, final String reason) {
        final ForecastProposalRun proposal = loadActionable(proposalId);
        proposal.setRejectedBy(currentUser);
        proposal.setRejectedAt(Instant.now());
        proposal.setRejectionReason(reason);
        proposal.setStatus(ForecastProposalStatus.REJECTED);
        return toDTO(proposalRepository.save(proposal), loadProducts(Set.of(proposal.getProductId())));
    }

    /**
     * Flips open proposals past their actionable window to EXPIRED so listings reflect the terminal
     * state (action-time guards already reject them either way). Returns the number expired.
     *
     * @return count of proposals transitioned to EXPIRED
     */
    @Transactional
    public int expireStaleProposals() {
        final int expired = proposalRepository.expireProposalsPastWindow(Instant.now());
        if (expired > 0) {
            log.info("[AI] Expired {} intraday proposals past their actionable window", expired);
        }
        return expired;
    }

    /**
     * Lists scoped proposals for a business date (defaults to today in the business zone).
     *
     * @param businessDate optional business date filter
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @param productId optional product filter
     * @return scoped proposal payloads
     */
    @Transactional(readOnly = true)
    public List<ForecastProposalRunDTO> listProposals(final LocalDate businessDate,
                                                      final Long centerId,
                                                      final Long warehouseId,
                                                      final Long productId) {
        final LocalDate effectiveDate = businessDate == null
                ? LocalDate.now(ZoneId.of(properties.getBusinessZone()))
                : businessDate;
        if (centerId != null && !scopeGuard.filterCenterIds(List.of(centerId)).contains(centerId)) {
            return List.of();
        }
        if (warehouseId != null && !scopeGuard.filterWarehouseIds(List.of(warehouseId)).contains(warehouseId)) {
            return List.of();
        }

        List<ForecastProposalRun> proposals = proposalRepository
                .findByBusinessDateOrderByRunSlotDescRecommendedQuantityDescIdAsc(effectiveDate);
        proposals = scopeGuard.filterByCenterWarehouseScope(
                proposals, ForecastProposalRun::getCenterId, ForecastProposalRun::getWarehouseId);

        final List<ForecastProposalRun> filtered = proposals.stream()
                .filter(p -> centerId == null || centerId.equals(p.getCenterId()))
                .filter(p -> warehouseId == null || warehouseId.equals(p.getWarehouseId()))
                .filter(p -> productId == null || productId.equals(p.getProductId()))
                .toList();

        final Map<Long, Product> products = loadProducts(
                filtered.stream().map(ForecastProposalRun::getProductId).collect(Collectors.toSet()));
        return filtered.stream().map(p -> toDTO(p, products)).toList();
    }

    private ForecastModel resolveForecastModel(final String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return defaultForecastModel;
        }
        return forecastModels.values().stream()
                .filter(model -> model.getModelId().equals(modelId))
                .findFirst()
                .orElse(defaultForecastModel);
    }

    private ForecastProposalRun loadActionable(final Long proposalId) {
        final ForecastProposalRun proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast proposal not found: " + proposalId));
        scopeGuard.assertCenterWarehouseAccess(proposal.getCenterId(), proposal.getWarehouseId());
        if (proposal.getStatus() != ForecastProposalStatus.PROPOSED) {
            throw new InvalidOperationException("Only open proposals can be approved or rejected");
        }
        if (!proposal.isActionable(Instant.now())) {
            throw new InvalidOperationException(
                    "Proposal is past its actionable window and is retained for history only");
        }
        return proposal;
    }

    private void applyComputation(final ForecastProposalRun proposal,
                                  final DimensionKey key,
                                  final LocalDate businessDate,
                                  final int runSlot,
                                  final Instant runAt,
                                  final Instant actionableUntil,
                                  final ForecastContext context,
                                  final ForecastResult computation) {
        proposal.setBusinessDate(businessDate);
        proposal.setRunSlot(runSlot);
        proposal.setRunAt(runAt);
        proposal.setActionableUntil(actionableUntil);
        proposal.setProductId(key.productId());
        proposal.setCenterId(key.centerId());
        proposal.setWarehouseId(key.warehouseId());
        proposal.setCurrentStockQuantity(context.currentStockQuantity());
        proposal.setSafetyStockQuantity(context.safetyStockQuantity());
        proposal.setRecommendedQuantity(computation.recommendedQuantity());
        proposal.setStatus(ForecastProposalStatus.PROPOSED);
        proposal.setTrailingSevenDayAverage(computation.trailingAverage());
        proposal.setSameWeekdayAverage(computation.sameWeekdayAverage());
        proposal.setWeightedDailyDemand(computation.weightedDailyDemand());
        proposal.setSevenDayForecastQuantity(computation.sevenDayForecastQuantity());
        proposal.setLeadTimeDays(computation.leadTimeDays());
        proposal.setLeadTimeDemandQuantity(computation.leadTimeDemandQuantity());
        proposal.setDemandEventCount(computation.demandEventCount());
        proposal.setModelVersion(computation.modelVersion());
        proposal.setExplanationSummary(computation.explanationSummary());
        proposal.setApprovedPurchaseOrder(null);
        proposal.setApprovedBy(null);
        proposal.setApprovedAt(null);
        proposal.setRejectedBy(null);
        proposal.setRejectedAt(null);
        proposal.setRejectionReason(null);
    }

    private void deleteStaleOpenProposals(final Map<DimensionKey, ForecastProposalRun> existingBySlot,
                                          final Set<DimensionKey> proposedKeys) {
        final List<ForecastProposalRun> stale = existingBySlot.entrySet().stream()
                .filter(entry -> !proposedKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(p -> p.getStatus() == ForecastProposalStatus.PROPOSED)
                .toList();
        if (!stale.isEmpty()) {
            proposalRepository.deleteAll(stale);
        }
    }

    private ForecastContext buildContext(final DimensionKey key,
                                         final LocalDate businessDate,
                                         final Product product,
                                         final Map<DimensionKey, Integer> liveStock,
                                         final Map<DimensionKey, List<DemandDataPoint>> demandHistory,
                                         final Map<DimensionKey, LeadTimeStats> leadTimes) {
        final LeadTimeStats stats = leadTimes.getOrDefault(
                key, LeadTimeStats.defaultFor(forecastTuning.getDefaultLeadTimeDays()));
        final LeadTimeInfo leadTimeInfo = new LeadTimeInfo(
                stats.totalLeadTimeHours(), stats.sampleCount(), forecastTuning.getDefaultLeadTimeDays());
        final ForecastParameters parameters = new ForecastParameters(
                forecastTuning.getTrailingAverageDays(),
                forecastTuning.getSameWeekdayLookbackWeeks(),
                FORECAST_HORIZON_DAYS,
                forecastTuning.getForecastHistoryDays(),
                TRAILING_WEIGHT,
                WEEKDAY_WEIGHT);
        return new ForecastContext(
                key.productId(),
                key.centerId(),
                key.warehouseId(),
                businessDate,
                liveStock.getOrDefault(key, 0),
                safetyStockOf(product),
                demandHistory.getOrDefault(key, List.of()),
                leadTimeInfo,
                parameters);
    }

    private Map<DimensionKey, Integer> loadLiveAvailableStock() {
        final String sql = """
                SELECT i.product_id,
                       w.center_id,
                       l.warehouse_id,
                       i.quantity,
                       i.reserved_quantity,
                       i.status
                FROM inventory i
                JOIN locations l ON l.id = i.location_id
                JOIN warehouses w ON w.id = l.warehouse_id
                """;
        final Map<DimensionKey, Integer> available = new HashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource(), rs -> {
            final DimensionKey key = new DimensionKey(
                    rs.getLong("product_id"), rs.getLong("center_id"), rs.getLong("warehouse_id"));
            final int quantity = rs.getInt("quantity");
            final int reserved = rs.getInt("reserved_quantity");
            final InventoryStatus status = InventoryStatus.valueOf(rs.getString("status"));
            final int delta = status == InventoryStatus.QUARANTINE ? 0 : Math.max(quantity - reserved, 0);
            available.merge(key, delta, Integer::sum);
        });
        return available;
    }

    private Map<DimensionKey, List<DemandDataPoint>> loadLiveDemandHistory(final LocalDate from, final LocalDate to) {
        if (to.isBefore(from)) {
            return Map.of();
        }
        final String sql = """
                SELECT o.outbound_date,
                       t.product_id,
                       w.center_id,
                       l.warehouse_id,
                       t.quantity
                FROM inventory_transactions t
                JOIN outbounds o ON o.id = t.reference_id
                JOIN locations l ON l.id = t.location_id
                JOIN warehouses w ON w.id = l.warehouse_id
                WHERE t.type = 'OUTBOUND'
                  AND o.status = 'CONFIRMED'
                  AND o.outbound_date BETWEEN :fromDate AND :toDate
                """;
        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);

        final Map<DimensionKey, Map<LocalDate, int[]>> byDimensionAndDate = new HashMap<>();
        jdbcTemplate.query(sql, parameters, rs -> {
            final DimensionKey key = new DimensionKey(
                    rs.getLong("product_id"), rs.getLong("center_id"), rs.getLong("warehouse_id"));
            final LocalDate date = rs.getObject("outbound_date", LocalDate.class);
            final int quantity = rs.getInt("quantity");
            final int[] accumulator = byDimensionAndDate
                    .computeIfAbsent(key, ignored -> new HashMap<>())
                    .computeIfAbsent(date, ignored -> new int[2]);
            accumulator[0] += quantity;
            accumulator[1] += 1;
        });

        final Map<DimensionKey, List<DemandDataPoint>> demandHistory = new HashMap<>();
        for (final Map.Entry<DimensionKey, Map<LocalDate, int[]>> entry : byDimensionAndDate.entrySet()) {
            final List<DemandDataPoint> points = new ArrayList<>();
            for (final Map.Entry<LocalDate, int[]> dateEntry : entry.getValue().entrySet()) {
                points.add(new DemandDataPoint(dateEntry.getKey(), dateEntry.getValue()[0], dateEntry.getValue()[1]));
            }
            demandHistory.put(entry.getKey(), points);
        }
        return demandHistory;
    }

    private Map<DimensionKey, LeadTimeStats> loadLeadTimeStats(final LocalDate from, final LocalDate to) {
        if (to.isBefore(from)) {
            return Map.of();
        }
        final String sql = """
                SELECT product_id,
                       center_id,
                       warehouse_id,
                       SUM(total_lead_time_hours) AS total_lead_time_hours,
                       SUM(lead_time_sample_count) AS total_samples
                FROM analytics.daily_purchase_order_lead_time
                WHERE business_date BETWEEN :fromDate AND :toDate
                  AND lead_time_sample_count > 0
                GROUP BY product_id, center_id, warehouse_id
                """;
        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);
        final Map<DimensionKey, LeadTimeStats> leadTimes = new HashMap<>();
        jdbcTemplate.query(sql, parameters, rs -> {
            final DimensionKey key = new DimensionKey(
                    rs.getLong("product_id"), rs.getLong("center_id"), rs.getLong("warehouse_id"));
            leadTimes.put(key, new LeadTimeStats(rs.getLong("total_lead_time_hours"), rs.getInt("total_samples")));
        });
        return leadTimes;
    }

    private Map<Long, Product> loadProducts(final Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        final Map<Long, Product> products = new HashMap<>();
        for (final Product product : productRepository.findAllById(productIds)) {
            products.put(product.getId(), product);
        }
        return products;
    }

    private Map<DimensionKey, ForecastProposalRun> indexBySlot(final List<ForecastProposalRun> proposals) {
        final Map<DimensionKey, ForecastProposalRun> indexed = new LinkedHashMap<>();
        for (final ForecastProposalRun proposal : proposals) {
            indexed.put(
                    new DimensionKey(proposal.getProductId(), proposal.getCenterId(), proposal.getWarehouseId()),
                    proposal);
        }
        return indexed;
    }

    private int safetyStockOf(final Product product) {
        return product.getSafetyStockQuantity() == null ? 0 : product.getSafetyStockQuantity();
    }

    private String appendExplanation(final String existing, final String addition) {
        return (existing == null ? "" : existing + " ") + addition;
    }

    private ForecastProposalRunDTO toDTO(final ForecastProposalRun proposal, final Map<Long, Product> products) {
        final Product product = products.get(proposal.getProductId());
        final PurchaseOrder po = proposal.getApprovedPurchaseOrder();
        return new ForecastProposalRunDTO(
                proposal.getId(),
                proposal.getBusinessDate(),
                proposal.getRunSlot(),
                proposal.getRunAt(),
                proposal.getActionableUntil(),
                proposal.isActionable(Instant.now()),
                proposal.getProductId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getBarcode(),
                proposal.getCenterId(),
                proposal.getWarehouseId(),
                proposal.getStatus(),
                proposal.getCurrentStockQuantity(),
                proposal.getSafetyStockQuantity(),
                proposal.getRecommendedQuantity(),
                proposal.getSevenDayForecastQuantity(),
                proposal.getLeadTimeDays(),
                proposal.getLeadTimeDemandQuantity(),
                proposal.getTrailingSevenDayAverage(),
                proposal.getSameWeekdayAverage(),
                proposal.getWeightedDailyDemand(),
                proposal.getDemandEventCount(),
                proposal.getModelVersion(),
                proposal.getExplanationSummary(),
                po == null ? null : po.getId(),
                po == null ? null : po.getPoNumber(),
                proposal.getApprovedAt(),
                proposal.getApprovedBy() == null ? null : proposal.getApprovedBy().getId(),
                proposal.getRejectedAt(),
                proposal.getRejectedBy() == null ? null : proposal.getRejectedBy().getId(),
                proposal.getRejectionReason(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt());
    }

    private record DimensionKey(Long productId, Long centerId, Long warehouseId) {
    }

    private record LeadTimeStats(long totalLeadTimeHours, int sampleCount) {

        private static LeadTimeStats defaultFor(final int defaultLeadTimeDays) {
            return new LeadTimeStats((long) defaultLeadTimeDays * 24L, 1);
        }
    }
}
