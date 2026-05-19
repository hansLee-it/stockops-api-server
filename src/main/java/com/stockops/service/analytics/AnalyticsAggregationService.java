package com.stockops.service.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.analytics.AnalyticsDemandHistory;
import com.stockops.entity.analytics.AnalyticsExpiryWaste;
import com.stockops.entity.analytics.AnalyticsFillRateSource;
import com.stockops.entity.analytics.AnalyticsPurchaseOrderLeadTime;
import com.stockops.entity.analytics.AnalyticsStockPosition;
import com.stockops.repository.analytics.AnalyticsDemandHistoryRepository;
import com.stockops.repository.analytics.AnalyticsExpiryWasteRepository;
import com.stockops.repository.analytics.AnalyticsFillRateSourceRepository;
import com.stockops.repository.analytics.AnalyticsPurchaseOrderLeadTimeRepository;
import com.stockops.repository.analytics.AnalyticsStockPositionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the shared analytics read model inside the existing Spring Boot application.
 * The refresh logic persists deterministic daily aggregates so BI and AI consumers do not recompute domain joins ad hoc.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
public class AnalyticsAggregationService {

    private static final String OUTBOUND_STATUS_CONFIRMED = "CONFIRMED";
    private static final String INVENTORY_TRANSACTION_TYPE_INBOUND = "INBOUND";
    private static final String INVENTORY_TRANSACTION_TYPE_OUTBOUND = "OUTBOUND";
    private static final String AUTO_QUARANTINE_ACTION = "AUTO_QUARANTINE";
    private static final String INVENTORY_ENTITY_TYPE = "Inventory";

    private final AnalyticsAggregationProperties properties;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AnalyticsDemandHistoryRepository demandHistoryRepository;
    private final AnalyticsStockPositionRepository stockPositionRepository;
    private final AnalyticsExpiryWasteRepository expiryWasteRepository;
    private final AnalyticsPurchaseOrderLeadTimeRepository purchaseOrderLeadTimeRepository;
    private final AnalyticsFillRateSourceRepository fillRateSourceRepository;

    /**
     * Refreshes the configured incremental analytics window ending on the current business date.
     */
    @Transactional
    public void refreshIncrementalAggregates() {
        final LocalDate today = LocalDate.now(getBusinessZone());
        final LocalDate from = today.minusDays(Math.max(properties.getIncrementalLookbackDays() - 1L, 0L));
        refreshRange(from, today);
    }

    /**
     * Backfills the configured analytics window ending on the current business date.
     */
    @Transactional
    public void backfillConfiguredHistory() {
        final LocalDate today = LocalDate.now(getBusinessZone());
        final LocalDate from = today.minusDays(Math.max(properties.getBackfillDays() - 1L, 0L));
        refreshRange(from, today);
    }

    /**
     * Rebuilds analytics rows for the supplied business-date range.
     *
     * @param from inclusive business-date range start
     * @param to inclusive business-date range end
     */
    @Transactional
    public void refreshRange(final LocalDate from, final LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Analytics refresh range must be fully specified");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Analytics refresh end date must not be before start date");
        }

        log.info("Refreshing analytics aggregates for business dates {} to {}", from, to);

        demandHistoryRepository.deleteByBusinessDateBetween(from, to);
        stockPositionRepository.deleteByBusinessDateBetween(from, to);
        expiryWasteRepository.deleteByBusinessDateBetween(from, to);
        purchaseOrderLeadTimeRepository.deleteByBusinessDateBetween(from, to);
        fillRateSourceRepository.deleteByBusinessDateBetween(from, to);

        final Map<DimensionKey, StockBalance> currentStockByDimension = loadCurrentStockBalances();
        final List<DemandEvent> confirmedDemandEvents = loadConfirmedDemandEventsUpTo(to);
        final List<InventoryMovement> inventoryMovements = loadInventoryMovementsAfter(from);
        final List<ExpiryWasteEvent> expiryWasteEvents = loadExpiryWasteEvents(from, to);
        final List<PurchaseOrderAnalyticsRow> purchaseOrderRows = loadPurchaseOrderAnalyticsRows(from, to);

        demandHistoryRepository.saveAll(buildDemandRows(from, to, currentStockByDimension.keySet(), confirmedDemandEvents));
        stockPositionRepository.saveAll(buildStockPositionRows(from, to, currentStockByDimension, inventoryMovements));
        expiryWasteRepository.saveAll(buildExpiryWasteRows(expiryWasteEvents));
        purchaseOrderLeadTimeRepository.saveAll(buildPurchaseOrderLeadTimeRows(purchaseOrderRows));
        fillRateSourceRepository.saveAll(buildFillRateRows(purchaseOrderRows));

        log.info("Analytics refresh completed for business dates {} to {}", from, to);
    }

    private ZoneId getBusinessZone() {
        return ZoneId.of(properties.getBusinessZone());
    }

    private Map<DimensionKey, StockBalance> loadCurrentStockBalances() {
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

        final Map<DimensionKey, StockBalance> balances = new HashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource(), rs -> {
            final DimensionKey key = new DimensionKey(
                    rs.getLong("product_id"),
                    rs.getLong("center_id"),
                    rs.getLong("warehouse_id"));
            final StockBalance balance = balances.computeIfAbsent(key, ignored -> new StockBalance());
            final int quantity = rs.getInt("quantity");
            final int reservedQuantity = rs.getInt("reserved_quantity");
            final InventoryStatus status = InventoryStatus.valueOf(rs.getString("status"));

            balance.onHandQuantity += quantity;
            balance.reservedQuantity += reservedQuantity;
            if (status == InventoryStatus.QUARANTINE) {
                balance.quarantinedQuantity += quantity;
            } else {
                balance.availableQuantity += Math.max(quantity - reservedQuantity, 0);
            }
        });
        return balances;
    }

    private List<DemandEvent> loadConfirmedDemandEventsUpTo(final LocalDate to) {
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
                WHERE t.type = :transactionType
                  AND o.status = :confirmedStatus
                  AND o.outbound_date <= :toDate
                """;

        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("transactionType", INVENTORY_TRANSACTION_TYPE_OUTBOUND)
                .addValue("confirmedStatus", OUTBOUND_STATUS_CONFIRMED)
                .addValue("toDate", to);

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new DemandEvent(
                rs.getObject("outbound_date", LocalDate.class),
                new DimensionKey(rs.getLong("product_id"), rs.getLong("center_id"), rs.getLong("warehouse_id")),
                rs.getInt("quantity")));
    }

    private List<InventoryMovement> loadInventoryMovementsAfter(final LocalDate from) {
        final String sql = """
                SELECT t.created_at,
                       t.product_id,
                       w.center_id,
                       l.warehouse_id,
                       t.type,
                       t.quantity
                FROM inventory_transactions t
                JOIN locations l ON l.id = t.location_id
                JOIN warehouses w ON w.id = l.warehouse_id
                WHERE t.created_at >= :fromInstant
                """;

        final Instant fromInstant = from.plusDays(1).atStartOfDay(getBusinessZone()).toInstant();
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("fromInstant", Timestamp.from(fromInstant)),
                (rs, rowNum) -> new InventoryMovement(
                        rs.getTimestamp("created_at").toInstant().atZone(getBusinessZone()).toLocalDate(),
                        new DimensionKey(rs.getLong("product_id"), rs.getLong("center_id"), rs.getLong("warehouse_id")),
                        signedDelta(rs.getString("type"), rs.getInt("quantity"))));
    }

    private List<ExpiryWasteEvent> loadExpiryWasteEvents(final LocalDate from, final LocalDate to) {
        final String sql = """
                SELECT a.performed_at,
                       i.product_id,
                       w.center_id,
                       l.warehouse_id,
                       i.lot_id,
                       i.quantity
                FROM audit_logs a
                JOIN inventory i ON i.id = a.entity_id
                JOIN locations l ON l.id = i.location_id
                JOIN warehouses w ON w.id = l.warehouse_id
                WHERE a.entity_type = :entityType
                  AND a.action = :action
                  AND a.performed_at BETWEEN :fromInstant AND :toInstant
                """;

        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("entityType", INVENTORY_ENTITY_TYPE)
                .addValue("action", AUTO_QUARANTINE_ACTION)
                .addValue("fromInstant", Timestamp.from(from.atStartOfDay(getBusinessZone()).toInstant()))
                .addValue("toInstant", Timestamp.from(to.plusDays(1).atStartOfDay(getBusinessZone()).minusNanos(1).toInstant()));

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new ExpiryWasteEvent(
                rs.getTimestamp("performed_at").toInstant().atZone(getBusinessZone()).toLocalDate(),
                new DimensionKey(rs.getLong("product_id"), rs.getLong("center_id"), rs.getLong("warehouse_id")),
                rs.getLong("lot_id"),
                rs.getInt("quantity")));
    }

    private List<PurchaseOrderAnalyticsRow> loadPurchaseOrderAnalyticsRows(final LocalDate from, final LocalDate to) {
        final String sql = """
                SELECT po.id AS purchase_order_id,
                       po.requested_at,
                       po.erp_responded_at,
                       po.requesting_center_id,
                       po.target_warehouse_id,
                       poi.product_id,
                       poi.requested_quantity,
                       COALESCE(poi.accepted_quantity, 0) AS accepted_quantity,
                       COALESCE(poi.cancelled_quantity, 0) AS cancelled_quantity,
                       COALESCE(SUM(psi.shipped_quantity), 0) AS shipped_quantity
                FROM purchase_orders po
                JOIN purchase_order_items poi ON poi.purchase_order_id = po.id
                LEFT JOIN purchase_order_shipment_items psi ON psi.purchase_order_item_id = poi.id
                WHERE po.status <> 'DRAFT'
                  AND po.requested_at IS NOT NULL
                  AND po.requested_at BETWEEN :fromDateTime AND :toDateTime
                GROUP BY po.id,
                         po.requested_at,
                         po.erp_responded_at,
                         po.requesting_center_id,
                         po.target_warehouse_id,
                         poi.product_id,
                         poi.requested_quantity,
                         poi.accepted_quantity,
                         poi.cancelled_quantity
                """;

        final MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fromDateTime", Timestamp.valueOf(from.atStartOfDay()))
                .addValue("toDateTime", Timestamp.valueOf(to.plusDays(1).atStartOfDay().minusNanos(1)));

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> mapPurchaseOrderAnalyticsRow(rs));
    }

    private List<AnalyticsDemandHistory> buildDemandRows(final LocalDate from,
                                                         final LocalDate to,
                                                         final Set<DimensionKey> stockDimensions,
                                                         final List<DemandEvent> confirmedDemandEvents) {
        final Map<AnalyticsDateKey, DemandAccumulator> dailyDemand = new HashMap<>();
        final Set<DimensionKey> allDimensions = new HashSet<>(stockDimensions);
        final Map<DimensionKey, List<DemandEvent>> eventsByDimension = new HashMap<>();

        for (final DemandEvent event : confirmedDemandEvents) {
            allDimensions.add(event.dimensionKey());
            eventsByDimension.computeIfAbsent(event.dimensionKey(), ignored -> new ArrayList<>()).add(event);
            if (!event.businessDate().isBefore(from) && !event.businessDate().isAfter(to)) {
                final AnalyticsDateKey key = new AnalyticsDateKey(event.businessDate(), event.dimensionKey());
                final DemandAccumulator accumulator = dailyDemand.computeIfAbsent(key, ignored -> new DemandAccumulator());
                accumulator.confirmedOutboundQuantity += event.quantity();
                accumulator.confirmedOutboundEventCount++;
            }
        }

        final List<AnalyticsDemandHistory> rows = new ArrayList<>();
        for (final DimensionKey dimension : allDimensions) {
            final List<DemandEvent> history = eventsByDimension.getOrDefault(dimension, List.of()).stream()
                    .sorted(Comparator.comparing(DemandEvent::businessDate))
                    .toList();
            int historyIndex = 0;
            int cumulativeEventCount = 0;

            for (LocalDate businessDate = from; !businessDate.isAfter(to); businessDate = businessDate.plusDays(1)) {
                while (historyIndex < history.size() && !history.get(historyIndex).businessDate().isAfter(businessDate)) {
                    cumulativeEventCount++;
                    historyIndex++;
                }

                final DemandAccumulator accumulator = dailyDemand.getOrDefault(
                        new AnalyticsDateKey(businessDate, dimension),
                        new DemandAccumulator());
                final AnalyticsDemandHistory row = new AnalyticsDemandHistory();
                row.setBusinessDate(businessDate);
                row.setProductId(dimension.productId());
                row.setCenterId(dimension.centerId());
                row.setWarehouseId(dimension.warehouseId());
                row.setConfirmedOutboundQuantity(accumulator.confirmedOutboundQuantity);
                row.setConfirmedOutboundEventCount(accumulator.confirmedOutboundEventCount);
                row.setInsufficientHistory(cumulativeEventCount == 0);
                rows.add(row);
            }
        }
        return rows;
    }

    private List<AnalyticsStockPosition> buildStockPositionRows(final LocalDate from,
                                                                final LocalDate to,
                                                                final Map<DimensionKey, StockBalance> currentStockByDimension,
                                                                final List<InventoryMovement> inventoryMovements) {
        final Map<AnalyticsDateKey, Integer> deltasByDateAndDimension = new HashMap<>();
        for (final InventoryMovement movement : inventoryMovements) {
            final AnalyticsDateKey key = new AnalyticsDateKey(movement.businessDate(), movement.dimensionKey());
            deltasByDateAndDimension.merge(key, movement.quantityDelta(), Integer::sum);
        }

        final LocalDate today = LocalDate.now(getBusinessZone());
        final List<AnalyticsStockPosition> rows = new ArrayList<>();
        for (final Map.Entry<DimensionKey, StockBalance> entry : currentStockByDimension.entrySet()) {
            final DimensionKey dimension = entry.getKey();
            final StockBalance running = entry.getValue().copy();

            if (to.isAfter(today)) {
                continue;
            }

            final Map<LocalDate, StockBalance> dailyBalances = new HashMap<>();
            dailyBalances.put(today, running.copy());
            for (LocalDate date = today.minusDays(1); !date.isBefore(from); date = date.minusDays(1)) {
                final Integer nextDayDelta = deltasByDateAndDimension.get(new AnalyticsDateKey(date.plusDays(1), dimension));
                if (nextDayDelta != null) {
                    running.onHandQuantity -= nextDayDelta;
                    running.availableQuantity -= nextDayDelta;
                }
                dailyBalances.put(date, running.copy());
            }

            for (LocalDate businessDate = from; !businessDate.isAfter(to); businessDate = businessDate.plusDays(1)) {
                final StockBalance balance = dailyBalances.getOrDefault(businessDate, running.copy());
                final AnalyticsStockPosition row = new AnalyticsStockPosition();
                row.setBusinessDate(businessDate);
                row.setProductId(dimension.productId());
                row.setCenterId(dimension.centerId());
                row.setWarehouseId(dimension.warehouseId());
                row.setOnHandQuantity(balance.onHandQuantity);
                row.setAvailableQuantity(Math.max(balance.availableQuantity, 0));
                row.setReservedQuantity(Math.max(balance.reservedQuantity, 0));
                row.setQuarantinedQuantity(Math.max(balance.quarantinedQuantity, 0));
                rows.add(row);
            }
        }

        return rows;
    }

    private List<AnalyticsExpiryWaste> buildExpiryWasteRows(final List<ExpiryWasteEvent> expiryWasteEvents) {
        final Map<AnalyticsDateKey, ExpiryWasteAccumulator> grouped = new HashMap<>();
        for (final ExpiryWasteEvent event : expiryWasteEvents) {
            final AnalyticsDateKey key = new AnalyticsDateKey(event.businessDate(), event.dimensionKey());
            final ExpiryWasteAccumulator accumulator = grouped.computeIfAbsent(key, ignored -> new ExpiryWasteAccumulator());
            accumulator.quarantinedQuantity += event.quantity();
            accumulator.quarantinedLotIds.add(event.lotId());
        }

        final List<AnalyticsExpiryWaste> rows = new ArrayList<>();
        for (final Map.Entry<AnalyticsDateKey, ExpiryWasteAccumulator> entry : grouped.entrySet()) {
            final AnalyticsExpiryWaste row = new AnalyticsExpiryWaste();
            row.setBusinessDate(entry.getKey().businessDate());
            row.setProductId(entry.getKey().dimensionKey().productId());
            row.setCenterId(entry.getKey().dimensionKey().centerId());
            row.setWarehouseId(entry.getKey().dimensionKey().warehouseId());
            row.setQuarantinedQuantity(entry.getValue().quarantinedQuantity);
            row.setQuarantinedLotCount(entry.getValue().quarantinedLotIds.size());
            rows.add(row);
        }
        return rows;
    }

    private List<AnalyticsPurchaseOrderLeadTime> buildPurchaseOrderLeadTimeRows(
            final List<PurchaseOrderAnalyticsRow> purchaseOrderRows) {
        final Map<AnalyticsDateKey, PurchaseOrderLeadTimeAccumulator> grouped = new HashMap<>();
        final Set<OrderAggregationKey> countedOrders = new HashSet<>();
        final Set<OrderAggregationKey> sampledOrders = new HashSet<>();

        for (final PurchaseOrderAnalyticsRow row : purchaseOrderRows) {
            final AnalyticsDateKey key = new AnalyticsDateKey(row.businessDate(), row.dimensionKey());
            final PurchaseOrderLeadTimeAccumulator accumulator = grouped.computeIfAbsent(
                    key,
                    ignored -> new PurchaseOrderLeadTimeAccumulator());

            final OrderAggregationKey orderAggregationKey = new OrderAggregationKey(key, row.purchaseOrderId());
            if (countedOrders.add(orderAggregationKey)) {
                accumulator.purchaseOrderCount++;
            }

            if (row.leadTimeHours() != null && sampledOrders.add(orderAggregationKey)) {
                accumulator.leadTimeSampleCount++;
                accumulator.totalLeadTimeHours += row.leadTimeHours();
            }
        }

        final List<AnalyticsPurchaseOrderLeadTime> rows = new ArrayList<>();
        for (final Map.Entry<AnalyticsDateKey, PurchaseOrderLeadTimeAccumulator> entry : grouped.entrySet()) {
            final AnalyticsPurchaseOrderLeadTime row = new AnalyticsPurchaseOrderLeadTime();
            row.setBusinessDate(entry.getKey().businessDate());
            row.setProductId(entry.getKey().dimensionKey().productId());
            row.setCenterId(entry.getKey().dimensionKey().centerId());
            row.setWarehouseId(entry.getKey().dimensionKey().warehouseId());
            row.setPurchaseOrderCount(entry.getValue().purchaseOrderCount);
            row.setLeadTimeSampleCount(entry.getValue().leadTimeSampleCount);
            row.setTotalLeadTimeHours(entry.getValue().totalLeadTimeHours);
            rows.add(row);
        }
        return rows;
    }

    private List<AnalyticsFillRateSource> buildFillRateRows(final List<PurchaseOrderAnalyticsRow> purchaseOrderRows) {
        final Map<AnalyticsDateKey, FillRateAccumulator> grouped = new HashMap<>();
        final Set<OrderAggregationKey> countedOrders = new HashSet<>();

        for (final PurchaseOrderAnalyticsRow row : purchaseOrderRows) {
            final AnalyticsDateKey key = new AnalyticsDateKey(row.businessDate(), row.dimensionKey());
            final FillRateAccumulator accumulator = grouped.computeIfAbsent(key, ignored -> new FillRateAccumulator());
            final OrderAggregationKey orderAggregationKey = new OrderAggregationKey(key, row.purchaseOrderId());
            if (countedOrders.add(orderAggregationKey)) {
                accumulator.purchaseOrderCount++;
            }

            accumulator.requestedQuantity += row.requestedQuantity();
            accumulator.acceptedQuantity += row.acceptedQuantity();
            accumulator.cancelledQuantity += row.cancelledQuantity();
            accumulator.shippedQuantity += row.shippedQuantity();
        }

        final List<AnalyticsFillRateSource> rows = new ArrayList<>();
        for (final Map.Entry<AnalyticsDateKey, FillRateAccumulator> entry : grouped.entrySet()) {
            final AnalyticsFillRateSource row = new AnalyticsFillRateSource();
            row.setBusinessDate(entry.getKey().businessDate());
            row.setProductId(entry.getKey().dimensionKey().productId());
            row.setCenterId(entry.getKey().dimensionKey().centerId());
            row.setWarehouseId(entry.getKey().dimensionKey().warehouseId());
            row.setPurchaseOrderCount(entry.getValue().purchaseOrderCount);
            row.setRequestedQuantity(entry.getValue().requestedQuantity);
            row.setAcceptedQuantity(entry.getValue().acceptedQuantity);
            row.setCancelledQuantity(entry.getValue().cancelledQuantity);
            row.setShippedQuantity(entry.getValue().shippedQuantity);
            rows.add(row);
        }
        return rows;
    }

    private PurchaseOrderAnalyticsRow mapPurchaseOrderAnalyticsRow(final ResultSet rs) throws SQLException {
        final LocalDateTime requestedAt = rs.getTimestamp("requested_at").toLocalDateTime();
        final Timestamp respondedAtTimestamp = rs.getTimestamp("erp_responded_at");
        final LocalDateTime respondedAt = respondedAtTimestamp == null ? null : respondedAtTimestamp.toLocalDateTime();
        final Long warehouseId = rs.getObject("target_warehouse_id") == null ? 0L : rs.getLong("target_warehouse_id");

        return new PurchaseOrderAnalyticsRow(
                rs.getLong("purchase_order_id"),
                requestedAt.toLocalDate(),
                new DimensionKey(rs.getLong("product_id"), rs.getLong("requesting_center_id"), warehouseId),
                rs.getInt("requested_quantity"),
                rs.getInt("accepted_quantity"),
                rs.getInt("cancelled_quantity"),
                rs.getInt("shipped_quantity"),
                respondedAt == null ? null : Duration.between(requestedAt, respondedAt).toHours());
    }

    private int signedDelta(final String transactionType, final int quantity) {
        if (INVENTORY_TRANSACTION_TYPE_INBOUND.equalsIgnoreCase(transactionType)) {
            return quantity;
        }
        if (INVENTORY_TRANSACTION_TYPE_OUTBOUND.equalsIgnoreCase(transactionType)) {
            return -quantity;
        }
        return 0;
    }

    private record DimensionKey(Long productId, Long centerId, Long warehouseId) {
    }

    private record AnalyticsDateKey(LocalDate businessDate, DimensionKey dimensionKey) {
    }

    private record OrderAggregationKey(AnalyticsDateKey analyticsDateKey, Long purchaseOrderId) {
    }

    private record DemandEvent(LocalDate businessDate, DimensionKey dimensionKey, int quantity) {
    }

    private record InventoryMovement(LocalDate businessDate, DimensionKey dimensionKey, int quantityDelta) {
    }

    private record ExpiryWasteEvent(LocalDate businessDate, DimensionKey dimensionKey, Long lotId, int quantity) {
    }

    private record PurchaseOrderAnalyticsRow(Long purchaseOrderId,
                                             LocalDate businessDate,
                                             DimensionKey dimensionKey,
                                             int requestedQuantity,
                                             int acceptedQuantity,
                                             int cancelledQuantity,
                                             int shippedQuantity,
                                             Long leadTimeHours) {
    }

    private static final class DemandAccumulator {
        private int confirmedOutboundQuantity;
        private int confirmedOutboundEventCount;
    }

    private static final class StockBalance {
        private int onHandQuantity;
        private int availableQuantity;
        private int reservedQuantity;
        private int quarantinedQuantity;

        private StockBalance copy() {
            final StockBalance copy = new StockBalance();
            copy.onHandQuantity = onHandQuantity;
            copy.availableQuantity = availableQuantity;
            copy.reservedQuantity = reservedQuantity;
            copy.quarantinedQuantity = quarantinedQuantity;
            return copy;
        }
    }

    private static final class ExpiryWasteAccumulator {
        private int quarantinedQuantity;
        private final Set<Long> quarantinedLotIds = new HashSet<>();
    }

    private static final class PurchaseOrderLeadTimeAccumulator {
        private int purchaseOrderCount;
        private int leadTimeSampleCount;
        private long totalLeadTimeHours;
    }

    private static final class FillRateAccumulator {
        private int purchaseOrderCount;
        private int requestedQuantity;
        private int acceptedQuantity;
        private int cancelledQuantity;
        private int shippedQuantity;
    }

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationService.class);

    public AnalyticsAggregationService(final AnalyticsAggregationProperties properties, final NamedParameterJdbcTemplate jdbcTemplate, final AnalyticsDemandHistoryRepository demandHistoryRepository, final AnalyticsStockPositionRepository stockPositionRepository, final AnalyticsExpiryWasteRepository expiryWasteRepository, final AnalyticsPurchaseOrderLeadTimeRepository purchaseOrderLeadTimeRepository, final AnalyticsFillRateSourceRepository fillRateSourceRepository) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.demandHistoryRepository = demandHistoryRepository;
        this.stockPositionRepository = stockPositionRepository;
        this.expiryWasteRepository = expiryWasteRepository;
        this.purchaseOrderLeadTimeRepository = purchaseOrderLeadTimeRepository;
        this.fillRateSourceRepository = fillRateSourceRepository;
    }
}
