package com.stockops.service.ai;

import com.stockops.dto.AIRecommendationDTO;
import com.stockops.entity.Product;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.User;
import com.stockops.entity.ai.AIForecastSnapshot;
import com.stockops.entity.ai.AIRecommendation;
import com.stockops.entity.ai.AIRecommendationStatus;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.ai.AIForecastSnapshotRepository;
import com.stockops.repository.ai.AIRecommendationRepository;
import com.stockops.security.ScopeGuard;
import com.stockops.service.PurchaseOrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds and serves deterministic AI reorder recommendations from the analytics read model.
 * Forecasts remain fully explainable because every recommendation persists its snapshot inputs.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRecommendationService {

    private static final BigDecimal TRAILING_AVERAGE_WEIGHT = new BigDecimal("0.70");
    private static final BigDecimal WEEKDAY_LOOKBACK_WEIGHT = new BigDecimal("0.30");
    private static final int FORECAST_HORIZON_DAYS = 7;

    private final AIRecommendationProperties properties;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final AIForecastSnapshotRepository forecastSnapshotRepository;
    private final AIRecommendationRepository recommendationRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final ScopeGuard scopeGuard;

    /**
     * Generates or refreshes AI recommendation snapshots for the supplied business date.
     * Previously approved recommendations are preserved so approval history remains stable.
     *
     * @param businessDate business date to generate
     */
    @Transactional
    public void generateRecommendationsForBusinessDate(final LocalDate businessDate) {
        final Map<DimensionKey, ProductDimensionContext> contexts = loadDimensionContexts(businessDate);
        final Map<DimensionKey, AIForecastSnapshot> existingForecasts = indexForecasts(forecastSnapshotRepository.findByBusinessDate(businessDate));
        final Map<DimensionKey, AIRecommendation> existingRecommendations = indexRecommendations(
                recommendationRepository.findByBusinessDate(businessDate));
        final Set<DimensionKey> processedKeys = new HashSet<>();

        for (Map.Entry<DimensionKey, ProductDimensionContext> entry : contexts.entrySet()) {
            final DimensionKey key = entry.getKey();
            final ProductDimensionContext context = entry.getValue();
            final AIRecommendation existingRecommendation = existingRecommendations.get(key);

            if (isApproved(existingRecommendation)) {
                processedKeys.add(key);
                continue;
            }

            final ForecastComputation computation = computeForecast(context, businessDate);

            final AIForecastSnapshot forecastSnapshot = upsertForecastSnapshot(
                    existingForecasts.get(key),
                    key,
                    businessDate,
                    computation);
            forecastSnapshotRepository.save(forecastSnapshot);

            final AIRecommendation recommendation = upsertRecommendation(
                    existingRecommendation,
                    forecastSnapshot,
                    key,
                    context,
                    computation,
                    businessDate);
            recommendationRepository.save(recommendation);
            processedKeys.add(key);
        }

        deleteStaleUnapprovedSnapshots(existingRecommendations, existingForecasts, processedKeys);
        log.info("Generated {} AI recommendation snapshots for {}", processedKeys.size(), businessDate);
    }

    /**
     * Lists scoped recommendation snapshots for the requested filters.
     *
     * @param businessDate optional business date, defaults to today in the business timezone
     * @param centerId optional center filter
     * @param warehouseId optional warehouse filter
     * @param productId optional product filter
     * @return filtered recommendation payloads
     */
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> listRecommendations(final LocalDate businessDate,
                                                         final Long centerId,
                                                         final Long warehouseId,
                                                         final Long productId) {
        final LocalDate effectiveBusinessDate = businessDate == null ? LocalDate.now(getBusinessZone()) : businessDate;
        if (centerId != null && !scopeGuard.filterCenterIds(List.of(centerId)).contains(centerId)) {
            return List.of();
        }
        if (warehouseId != null && !scopeGuard.filterWarehouseIds(List.of(warehouseId)).contains(warehouseId)) {
            return List.of();
        }

        List<AIRecommendation> recommendations = recommendationRepository
                .findByBusinessDateOrderByRecommendedQuantityDescIdAsc(effectiveBusinessDate);
        recommendations = scopeGuard.filterByCenterWarehouseScope(
                recommendations,
                AIRecommendation::getCenterId,
                AIRecommendation::getWarehouseId);

        final List<AIRecommendation> filteredRecommendations = recommendations.stream()
                .filter(recommendation -> centerId == null || centerId.equals(recommendation.getCenterId()))
                .filter(recommendation -> warehouseId == null || warehouseId.equals(recommendation.getWarehouseId()))
                .filter(recommendation -> productId == null || productId.equals(recommendation.getProductId()))
                .toList();

        return toDTOs(filteredRecommendations);
    }

    /**
     * Approves one ready recommendation into a draft purchase order.
     * The method never submits or auto-accepts the draft downstream.
     *
     * @param recommendationId recommendation identifier
     * @param currentUser approving user
     * @return approved recommendation payload including the linked draft purchase order
     */
    @Transactional
    public AIRecommendationDTO approveRecommendation(final Long recommendationId, final User currentUser) {
        final AIRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("AI recommendation not found: " + recommendationId));
        scopeGuard.assertCenterWarehouseAccess(recommendation.getCenterId(), recommendation.getWarehouseId());

        if (recommendation.getStatus() != AIRecommendationStatus.READY_FOR_APPROVAL) {
            throw new InvalidOperationException("Only READY_FOR_APPROVAL recommendations can be approved");
        }
        if (recommendation.getRecommendedQuantity() == null || recommendation.getRecommendedQuantity() <= 0) {
            throw new InvalidOperationException("Recommendation quantity must be positive to create a draft purchase order");
        }

        PurchaseOrder purchaseOrder = purchaseOrderService.create(
                recommendation.getCenterId(),
                recommendation.getWarehouseId(),
                currentUser);
        purchaseOrder = purchaseOrderService.addItem(
                purchaseOrder.getId(),
                recommendation.getProductId(),
                recommendation.getRecommendedQuantity());

        recommendation.setApprovedPurchaseOrder(purchaseOrder);
        recommendation.setApprovedBy(currentUser);
        recommendation.setApprovedAt(Instant.now());
        recommendation.setStatus(AIRecommendationStatus.APPROVED_TO_DRAFT);
        recommendation.setExplanationSummary(appendApprovalExplanation(recommendation.getExplanationSummary(), purchaseOrder));

        final AIRecommendation savedRecommendation = recommendationRepository.save(recommendation);
        return toDTO(savedRecommendation, loadProducts(Set.of(savedRecommendation.getProductId())));
    }

    private Map<Long, Product> loadProducts(final Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        final Map<Long, Product> products = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            products.put(product.getId(), product);
        }
        return products;
    }

    private List<AIRecommendationDTO> toDTOs(final List<AIRecommendation> recommendations) {
        final Set<Long> productIds = recommendations.stream().map(AIRecommendation::getProductId).collect(java.util.stream.Collectors.toSet());
        final Map<Long, Product> products = loadProducts(productIds);
        return recommendations.stream().map(recommendation -> toDTO(recommendation, products)).toList();
    }

    private AIRecommendationDTO toDTO(final AIRecommendation recommendation, final Map<Long, Product> products) {
        final Product product = products.get(recommendation.getProductId());
        final AIForecastSnapshot forecastSnapshot = recommendation.getForecastSnapshot();
        return new AIRecommendationDTO(
                recommendation.getId(),
                recommendation.getBusinessDate(),
                recommendation.getProductId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getBarcode(),
                recommendation.getCenterId(),
                recommendation.getWarehouseId(),
                recommendation.getStatus(),
                recommendation.getCurrentStockQuantity(),
                recommendation.getSafetyStockQuantity(),
                recommendation.getRecommendedQuantity(),
                forecastSnapshot.getSevenDayForecastQuantity(),
                forecastSnapshot.getLeadTimeDays(),
                forecastSnapshot.getLeadTimeDemandQuantity(),
                forecastSnapshot.getTrailingSevenDayAverage(),
                forecastSnapshot.getSameWeekdayAverage(),
                forecastSnapshot.getWeightedDailyDemand(),
                forecastSnapshot.getDemandEventCount(),
                forecastSnapshot.isInsufficientHistory(),
                recommendation.getExplanationSummary(),
                recommendation.getApprovedPurchaseOrder() == null ? null : recommendation.getApprovedPurchaseOrder().getId(),
                recommendation.getApprovedPurchaseOrder() == null ? null : recommendation.getApprovedPurchaseOrder().getPoNumber(),
                recommendation.getApprovedAt(),
                recommendation.getApprovedBy() == null ? null : recommendation.getApprovedBy().getId(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt());
    }

    private Map<DimensionKey, ProductDimensionContext> loadDimensionContexts(final LocalDate businessDate) {
        final LocalDate demandHistoryFrom = businessDate.minusDays(properties.getForecastHistoryDays());
        final LocalDate demandHistoryTo = businessDate.minusDays(1);

        final List<DemandHistoryRow> demandRows = loadDemandHistoryRows(demandHistoryFrom, demandHistoryTo);
        final Map<DimensionKey, Integer> currentStockByDimension = loadCurrentStockByDimension(businessDate);
        final Map<DimensionKey, LeadTimeStats> leadTimeByDimension = loadLeadTimeByDimension(
                businessDate.minusDays(properties.getLeadTimeLookbackDays()),
                businessDate.minusDays(1));

        final Set<DimensionKey> dimensionKeys = new HashSet<>();
        dimensionKeys.addAll(currentStockByDimension.keySet());
        dimensionKeys.addAll(leadTimeByDimension.keySet());
        for (DemandHistoryRow demandRow : demandRows) {
            dimensionKeys.add(demandRow.dimensionKey());
        }

        final Map<Long, Product> products = loadProducts(dimensionKeys.stream().map(DimensionKey::productId).collect(java.util.stream.Collectors.toSet()));
        final Map<DimensionKey, List<DemandHistoryRow>> demandRowsByDimension = new HashMap<>();
        for (DemandHistoryRow demandRow : demandRows) {
            demandRowsByDimension.computeIfAbsent(demandRow.dimensionKey(), ignored -> new ArrayList<>()).add(demandRow);
        }

        final Map<DimensionKey, ProductDimensionContext> contexts = new LinkedHashMap<>();
        for (DimensionKey key : dimensionKeys.stream().sorted(DimensionKey::compareTo).toList()) {
            final Product product = products.get(key.productId());
            if (product == null) {
                continue;
            }

            contexts.put(key, new ProductDimensionContext(
                    key,
                    product,
                    currentStockByDimension.getOrDefault(key, 0),
                    leadTimeByDimension.getOrDefault(key, LeadTimeStats.defaultFor(properties.getDefaultLeadTimeDays())),
                    demandRowsByDimension.getOrDefault(key, List.of())));
        }
        return contexts;
    }

    private List<DemandHistoryRow> loadDemandHistoryRows(final LocalDate from, final LocalDate to) {
        if (to.isBefore(from)) {
            return List.of();
        }
        final String sql = """
                SELECT business_date,
                       product_id,
                       center_id,
                       warehouse_id,
                       confirmed_outbound_quantity,
                       confirmed_outbound_event_count
                FROM analytics.daily_demand_history
                WHERE business_date BETWEEN :fromDate AND :toDate
                """;

        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> mapDemandHistoryRow(rs));
    }

    private Map<DimensionKey, Integer> loadCurrentStockByDimension(final LocalDate businessDate) {
        final String sql = """
                SELECT sp.product_id,
                       sp.center_id,
                       sp.warehouse_id,
                       sp.available_quantity
                FROM analytics.daily_stock_position sp
                JOIN (
                    SELECT product_id,
                           center_id,
                           warehouse_id,
                           MAX(business_date) AS max_business_date
                    FROM analytics.daily_stock_position
                    WHERE business_date <= :businessDate
                    GROUP BY product_id, center_id, warehouse_id
                ) latest
                    ON latest.product_id = sp.product_id
                   AND latest.center_id = sp.center_id
                   AND latest.warehouse_id = sp.warehouse_id
                   AND latest.max_business_date = sp.business_date
                """;

        final Map<DimensionKey, Integer> currentStockByDimension = new HashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource("businessDate", businessDate), rs -> {
            final DimensionKey key = new DimensionKey(
                    rs.getLong("product_id"),
                    rs.getLong("center_id"),
                    rs.getLong("warehouse_id"));
            currentStockByDimension.put(key, rs.getInt("available_quantity"));
        });
        return currentStockByDimension;
    }

    private Map<DimensionKey, LeadTimeStats> loadLeadTimeByDimension(final LocalDate from, final LocalDate to) {
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

        final Map<DimensionKey, LeadTimeStats> leadTimeByDimension = new HashMap<>();
        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);
        jdbcTemplate.query(sql, parameters, rs -> {
            final DimensionKey key = new DimensionKey(
                    rs.getLong("product_id"),
                    rs.getLong("center_id"),
                    rs.getLong("warehouse_id"));
            final long totalLeadTimeHours = rs.getLong("total_lead_time_hours");
            final int samples = rs.getInt("total_samples");
            leadTimeByDimension.put(key, new LeadTimeStats(totalLeadTimeHours, samples));
        });
        return leadTimeByDimension;
    }

    private ForecastComputation computeForecast(final ProductDimensionContext context, final LocalDate businessDate) {
        final Map<LocalDate, DemandHistoryRow> demandByDate = new HashMap<>();
        for (DemandHistoryRow demandRow : context.demandRows()) {
            demandByDate.put(demandRow.businessDate(), demandRow);
        }

        final int trailingDays = properties.getTrailingAverageDays();
        int trailingQuantityTotal = 0;
        for (int dayOffset = trailingDays; dayOffset >= 1; dayOffset--) {
            trailingQuantityTotal += quantityForDate(demandByDate, businessDate.minusDays(dayOffset));
        }
        final BigDecimal trailingAverage = BigDecimal.valueOf(trailingQuantityTotal)
                .divide(BigDecimal.valueOf(trailingDays), 2, RoundingMode.HALF_UP);

        int demandEventCount = context.demandRows().stream().mapToInt(DemandHistoryRow::confirmedOutboundEventCount).sum();
        if (demandEventCount == 0) {
            return ForecastComputation.insufficientHistory(
                    trailingAverage,
                    context.leadTimeStats().resolvedLeadTimeDays(properties.getDefaultLeadTimeDays()),
                    properties.getForecastHistoryDays());
        }

        final List<BigDecimal> dailyForecasts = new ArrayList<>();
        BigDecimal weekdayAverageAccumulator = BigDecimal.ZERO;
        for (int forecastOffset = 0; forecastOffset < FORECAST_HORIZON_DAYS; forecastOffset++) {
            final LocalDate targetDate = businessDate.plusDays(forecastOffset);
            final BigDecimal weekdayAverage = sameWeekdayAverage(demandByDate, targetDate);
            weekdayAverageAccumulator = weekdayAverageAccumulator.add(weekdayAverage);
            final BigDecimal weightedDemand = trailingAverage.multiply(TRAILING_AVERAGE_WEIGHT)
                    .add(weekdayAverage.multiply(WEEKDAY_LOOKBACK_WEIGHT))
                    .setScale(2, RoundingMode.HALF_UP);
            dailyForecasts.add(weightedDemand);
        }

        final BigDecimal sameWeekdayAverage = weekdayAverageAccumulator
                .divide(BigDecimal.valueOf(FORECAST_HORIZON_DAYS), 2, RoundingMode.HALF_UP);
        final BigDecimal weightedDailyDemand = dailyForecasts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(FORECAST_HORIZON_DAYS), 2, RoundingMode.HALF_UP);
        final int sevenDayForecastQuantity = dailyForecasts.stream()
                .map(value -> value.setScale(0, RoundingMode.HALF_UP).intValue())
                .reduce(0, Integer::sum);
        final int leadTimeDays = context.leadTimeStats().resolvedLeadTimeDays(properties.getDefaultLeadTimeDays());
        final int leadTimeDemandQuantity = weightedDailyDemand
                .multiply(BigDecimal.valueOf(leadTimeDays))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        final int recommendedQuantity = Math.max(leadTimeDemandQuantity + context.safetyStockQuantity() - context.currentStockQuantity(), 0);
        final AIRecommendationStatus status = recommendedQuantity > 0
                ? AIRecommendationStatus.READY_FOR_APPROVAL
                : AIRecommendationStatus.NO_ACTION;
        final String explanationSummary = String.format(
                "Forecast=%s/day (70%% trailing-7 avg %s + 30%% weekday avg %s); leadTimeDays=%d; currentStock=%d; safetyStock=%d; recommended=%d",
                weightedDailyDemand,
                trailingAverage,
                sameWeekdayAverage,
                leadTimeDays,
                context.currentStockQuantity(),
                context.safetyStockQuantity(),
                recommendedQuantity);

        return new ForecastComputation(
                trailingAverage,
                sameWeekdayAverage,
                weightedDailyDemand,
                sevenDayForecastQuantity,
                leadTimeDays,
                leadTimeDemandQuantity,
                properties.getForecastHistoryDays(),
                demandEventCount,
                false,
                recommendedQuantity,
                status,
                explanationSummary);
    }

    private BigDecimal sameWeekdayAverage(final Map<LocalDate, DemandHistoryRow> demandByDate, final LocalDate targetDate) {
        BigDecimal total = BigDecimal.ZERO;
        int sampleCount = 0;
        for (int week = 1; week <= properties.getSameWeekdayLookbackWeeks(); week++) {
            final LocalDate lookbackDate = targetDate.minusWeeks(week);
            total = total.add(BigDecimal.valueOf(quantityForDate(demandByDate, lookbackDate)));
            sampleCount++;
        }
        if (sampleCount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(sampleCount), 2, RoundingMode.HALF_UP);
    }

    private int quantityForDate(final Map<LocalDate, DemandHistoryRow> demandByDate, final LocalDate businessDate) {
        return Optional.ofNullable(demandByDate.get(businessDate))
                .map(DemandHistoryRow::confirmedOutboundQuantity)
                .orElse(0);
    }

    private AIForecastSnapshot upsertForecastSnapshot(final AIForecastSnapshot existingSnapshot,
                                                     final DimensionKey key,
                                                     final LocalDate businessDate,
                                                     final ForecastComputation computation) {
        final AIForecastSnapshot snapshot = existingSnapshot == null ? new AIForecastSnapshot() : existingSnapshot;
        snapshot.setBusinessDate(businessDate);
        snapshot.setForecastStartDate(businessDate);
        snapshot.setForecastEndDate(businessDate.plusDays(FORECAST_HORIZON_DAYS - 1L));
        snapshot.setProductId(key.productId());
        snapshot.setCenterId(key.centerId());
        snapshot.setWarehouseId(key.warehouseId());
        snapshot.setTrailingSevenDayAverage(computation.trailingAverage());
        snapshot.setSameWeekdayAverage(computation.sameWeekdayAverage());
        snapshot.setWeightedDailyDemand(computation.weightedDailyDemand());
        snapshot.setSevenDayForecastQuantity(computation.sevenDayForecastQuantity());
        snapshot.setLeadTimeDays(computation.leadTimeDays());
        snapshot.setLeadTimeDemandQuantity(computation.leadTimeDemandQuantity());
        snapshot.setHistoryDaysConsidered(computation.historyDaysConsidered());
        snapshot.setDemandEventCount(computation.demandEventCount());
        snapshot.setInsufficientHistory(computation.insufficientHistory());
        snapshot.setExplanationSummary(computation.explanationSummary());
        return snapshot;
    }

    private AIRecommendation upsertRecommendation(final AIRecommendation existingRecommendation,
                                                  final AIForecastSnapshot forecastSnapshot,
                                                  final DimensionKey key,
                                                  final ProductDimensionContext context,
                                                  final ForecastComputation computation,
                                                  final LocalDate businessDate) {
        final AIRecommendation recommendation = existingRecommendation == null ? new AIRecommendation() : existingRecommendation;
        recommendation.setBusinessDate(businessDate);
        recommendation.setProductId(key.productId());
        recommendation.setCenterId(key.centerId());
        recommendation.setWarehouseId(key.warehouseId());
        recommendation.setForecastSnapshot(forecastSnapshot);
        recommendation.setCurrentStockQuantity(context.currentStockQuantity());
        recommendation.setSafetyStockQuantity(context.safetyStockQuantity());
        recommendation.setRecommendedQuantity(computation.recommendedQuantity());
        recommendation.setStatus(computation.status());
        recommendation.setExplanationSummary(computation.explanationSummary());
        recommendation.setApprovedAt(null);
        recommendation.setApprovedBy(null);
        recommendation.setApprovedPurchaseOrder(null);
        return recommendation;
    }

    private void deleteStaleUnapprovedSnapshots(final Map<DimensionKey, AIRecommendation> existingRecommendations,
                                                final Map<DimensionKey, AIForecastSnapshot> existingForecasts,
                                                final Set<DimensionKey> processedKeys) {
        final List<AIRecommendation> recommendationsToDelete = existingRecommendations.entrySet().stream()
                .filter(entry -> !processedKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(recommendation -> !isApproved(recommendation))
                .toList();
        if (!recommendationsToDelete.isEmpty()) {
            recommendationRepository.deleteAll(recommendationsToDelete);
        }

        final List<AIForecastSnapshot> forecastsToDelete = existingForecasts.entrySet().stream()
                .filter(entry -> !processedKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!forecastsToDelete.isEmpty()) {
            forecastSnapshotRepository.deleteAll(forecastsToDelete);
        }
    }

    private boolean isApproved(final AIRecommendation recommendation) {
        return recommendation != null
                && recommendation.getStatus() == AIRecommendationStatus.APPROVED_TO_DRAFT
                && recommendation.getApprovedPurchaseOrder() != null;
    }

    private String appendApprovalExplanation(final String existingExplanation, final PurchaseOrder purchaseOrder) {
        return (existingExplanation == null ? "" : existingExplanation + " ")
                + "Approved into draft purchase order " + purchaseOrder.getPoNumber();
    }

    private ZoneId getBusinessZone() {
        return ZoneId.of(properties.getBusinessZone());
    }

    private Map<DimensionKey, AIForecastSnapshot> indexForecasts(final List<AIForecastSnapshot> snapshots) {
        final Map<DimensionKey, AIForecastSnapshot> indexed = new HashMap<>();
        for (AIForecastSnapshot snapshot : snapshots) {
            indexed.put(new DimensionKey(snapshot.getProductId(), snapshot.getCenterId(), snapshot.getWarehouseId()), snapshot);
        }
        return indexed;
    }

    private Map<DimensionKey, AIRecommendation> indexRecommendations(final List<AIRecommendation> recommendations) {
        final Map<DimensionKey, AIRecommendation> indexed = new HashMap<>();
        for (AIRecommendation recommendation : recommendations) {
            indexed.put(new DimensionKey(recommendation.getProductId(), recommendation.getCenterId(), recommendation.getWarehouseId()), recommendation);
        }
        return indexed;
    }

    private DemandHistoryRow mapDemandHistoryRow(final ResultSet resultSet) throws SQLException {
        return new DemandHistoryRow(
                resultSet.getObject("business_date", LocalDate.class),
                new DimensionKey(resultSet.getLong("product_id"), resultSet.getLong("center_id"), resultSet.getLong("warehouse_id")),
                resultSet.getInt("confirmed_outbound_quantity"),
                resultSet.getInt("confirmed_outbound_event_count"));
    }

    private record ProductDimensionContext(
            DimensionKey key,
            Product product,
            int currentStockQuantity,
            LeadTimeStats leadTimeStats,
            List<DemandHistoryRow> demandRows) {

        private int safetyStockQuantity() {
            return product == null || product.getSafetyStockQuantity() == null ? 0 : product.getSafetyStockQuantity();
        }
    }

    private record DemandHistoryRow(
            LocalDate businessDate,
            DimensionKey dimensionKey,
            int confirmedOutboundQuantity,
            int confirmedOutboundEventCount) {
    }

    private record LeadTimeStats(long totalLeadTimeHours, int sampleCount) {

        private static LeadTimeStats defaultFor(final int defaultLeadTimeDays) {
            return new LeadTimeStats((long) defaultLeadTimeDays * 24L, 1);
        }

        private int resolvedLeadTimeDays(final int defaultLeadTimeDays) {
            if (sampleCount <= 0) {
                return Math.max(defaultLeadTimeDays, 1);
            }
            final BigDecimal averageHours = BigDecimal.valueOf(totalLeadTimeHours)
                    .divide(BigDecimal.valueOf(sampleCount), 2, RoundingMode.HALF_UP);
            return Math.max(averageHours.divide(BigDecimal.valueOf(24), 0, RoundingMode.CEILING).intValue(), 1);
        }
    }

    private record ForecastComputation(
            BigDecimal trailingAverage,
            BigDecimal sameWeekdayAverage,
            BigDecimal weightedDailyDemand,
            int sevenDayForecastQuantity,
            int leadTimeDays,
            int leadTimeDemandQuantity,
            int historyDaysConsidered,
            int demandEventCount,
            boolean insufficientHistory,
            int recommendedQuantity,
            AIRecommendationStatus status,
            String explanationSummary) {

        private static ForecastComputation insufficientHistory(final BigDecimal trailingAverage,
                                                               final int leadTimeDays,
                                                               final int historyDaysConsidered) {
            return new ForecastComputation(
                    trailingAverage,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    leadTimeDays,
                    0,
                    historyDaysConsidered,
                    0,
                    true,
                    0,
                    AIRecommendationStatus.INSUFFICIENT_HISTORY,
                    "No confirmed outbound history was available for deterministic forecasting.");
        }
    }

    private record DimensionKey(Long productId, Long centerId, Long warehouseId) implements Comparable<DimensionKey> {

        @Override
        public int compareTo(final DimensionKey other) {
            int productCompare = productId.compareTo(other.productId);
            if (productCompare != 0) {
                return productCompare;
            }
            int centerCompare = centerId.compareTo(other.centerId);
            if (centerCompare != 0) {
                return centerCompare;
            }
            return warehouseId.compareTo(other.warehouseId);
        }
    }
}
