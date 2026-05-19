package com.stockops.service.analytics;

import com.stockops.dto.analytics.AnalyticsQueryFilter;
import com.stockops.dto.analytics.ExpiryWasteReportResponse;
import com.stockops.dto.analytics.FillRateReportResponse;
import com.stockops.dto.analytics.PurchaseOrderLeadTimeReportResponse;
import com.stockops.dto.analytics.StockAgingReportResponse;
import com.stockops.dto.analytics.StockoutRateReportResponse;
import com.stockops.entity.Center;
import com.stockops.entity.Product;
import com.stockops.entity.Warehouse;
import com.stockops.entity.analytics.AnalyticsDemandHistory;
import com.stockops.entity.analytics.AnalyticsExpiryWaste;
import com.stockops.entity.analytics.AnalyticsFillRateSource;
import com.stockops.entity.analytics.AnalyticsPurchaseOrderLeadTime;
import com.stockops.entity.analytics.AnalyticsStockPosition;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.repository.analytics.AnalyticsDemandHistoryRepository;
import com.stockops.repository.analytics.AnalyticsExpiryWasteRepository;
import com.stockops.repository.analytics.AnalyticsFillRateSourceRepository;
import com.stockops.repository.analytics.AnalyticsPurchaseOrderLeadTimeRepository;
import com.stockops.repository.analytics.AnalyticsStockPositionRepository;
import com.stockops.security.ScopeGuard;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared scoped analytics query service for BI APIs and exports.
 * Computes metric DTOs once from the analytics read model so JSON, PDF, and XLSX surfaces stay aligned.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsReportingService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final AnalyticsDemandHistoryRepository analyticsDemandHistoryRepository;
    private final AnalyticsStockPositionRepository analyticsStockPositionRepository;
    private final AnalyticsExpiryWasteRepository analyticsExpiryWasteRepository;
    private final AnalyticsPurchaseOrderLeadTimeRepository analyticsPurchaseOrderLeadTimeRepository;
    private final AnalyticsFillRateSourceRepository analyticsFillRateSourceRepository;
    private final ProductRepository productRepository;
    private final CenterRepository centerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ScopeGuard scopeGuard;

    /**
     * Returns stock-aging analytics.
     *
     * @param filter scoped analytics filter
     * @return stock-aging response DTO
     */
    public StockAgingReportResponse getStockAgingReport(final AnalyticsQueryFilter filter) {
        final AnalyticsQueryFilter validatedFilter = validateFilter(filter);
        final List<AnalyticsStockPosition> positions = scopedAndDatedRows(
                loadStockPositions(validatedFilter),
                AnalyticsStockPosition::getCenterId,
                AnalyticsStockPosition::getWarehouseId,
                AnalyticsStockPosition::getBusinessDate,
                validatedFilter);
        final List<AnalyticsDemandHistory> demandHistory = scopedAndDatedRows(
                loadDemandHistory(validatedFilter),
                AnalyticsDemandHistory::getCenterId,
                AnalyticsDemandHistory::getWarehouseId,
                AnalyticsDemandHistory::getBusinessDate,
                validatedFilter);

        final Map<DimensionKey, AnalyticsStockPosition> latestPositions = new HashMap<>();
        for (final AnalyticsStockPosition position : positions) {
            latestPositions.merge(
                    new DimensionKey(position.getProductId(), position.getCenterId(), position.getWarehouseId()),
                    position,
                    (current, candidate) -> candidate.getBusinessDate().isAfter(current.getBusinessDate()) ? candidate : current);
        }

        final Map<DimensionKey, Integer> demandTotals = new HashMap<>();
        for (final AnalyticsDemandHistory demandRow : demandHistory) {
            demandTotals.merge(
                    new DimensionKey(demandRow.getProductId(), demandRow.getCenterId(), demandRow.getWarehouseId()),
                    defaultInt(demandRow.getConfirmedOutboundQuantity()),
                    Integer::sum);
        }

        final int observedDayCount = resolveObservedDayCount(
                validatedFilter,
                positions.stream().map(AnalyticsStockPosition::getBusinessDate).toList());
        final MetadataBundle metadataBundle = loadMetadata(latestPositions.keySet());
        final List<StockAgingReportResponse.StockAgingRow> rows = latestPositions.entrySet().stream()
                .map(entry -> toStockAgingRow(entry.getKey(), entry.getValue(), demandTotals.getOrDefault(entry.getKey(), 0), observedDayCount, metadataBundle))
                .sorted(stockRowComparator())
                .toList();

        int zeroToThirtyQuantity = 0;
        int thirtyOneToSixtyQuantity = 0;
        int sixtyOneToNinetyQuantity = 0;
        int overNinetyQuantity = 0;
        int noDemandQuantity = 0;
        int totalAvailableQuantity = 0;
        for (final StockAgingReportResponse.StockAgingRow row : rows) {
            totalAvailableQuantity += row.availableQuantity();
            switch (row.agingBucket()) {
                case "0-30_DAYS" -> zeroToThirtyQuantity += row.availableQuantity();
                case "31-60_DAYS" -> thirtyOneToSixtyQuantity += row.availableQuantity();
                case "61-90_DAYS" -> sixtyOneToNinetyQuantity += row.availableQuantity();
                case "90+_DAYS" -> overNinetyQuantity += row.availableQuantity();
                default -> noDemandQuantity += row.availableQuantity();
            }
        }

        return new StockAgingReportResponse(
                new StockAgingReportResponse.StockAgingSummary(
                        rows.size(),
                        totalAvailableQuantity,
                        zeroToThirtyQuantity,
                        thirtyOneToSixtyQuantity,
                        sixtyOneToNinetyQuantity,
                        overNinetyQuantity,
                        noDemandQuantity),
                rows);
    }

    /**
     * Returns stockout-rate analytics.
     *
     * @param filter scoped analytics filter
     * @return stockout-rate response DTO
     */
    public StockoutRateReportResponse getStockoutRateReport(final AnalyticsQueryFilter filter) {
        final AnalyticsQueryFilter validatedFilter = validateFilter(filter);
        final List<AnalyticsStockPosition> positions = scopedAndDatedRows(
                loadStockPositions(validatedFilter),
                AnalyticsStockPosition::getCenterId,
                AnalyticsStockPosition::getWarehouseId,
                AnalyticsStockPosition::getBusinessDate,
                validatedFilter);

        final Map<DimensionKey, List<AnalyticsStockPosition>> grouped = groupByDimension(
                positions,
                row -> new DimensionKey(row.getProductId(), row.getCenterId(), row.getWarehouseId()));
        final MetadataBundle metadataBundle = loadMetadata(grouped.keySet());
        final List<StockoutRateReportResponse.StockoutRateRow> rows = new ArrayList<>();
        int totalObservedDayCount = 0;
        int totalStockoutDayCount = 0;

        for (final Map.Entry<DimensionKey, List<AnalyticsStockPosition>> entry : grouped.entrySet()) {
            final List<AnalyticsStockPosition> dimensionRows = entry.getValue().stream()
                    .sorted(Comparator.comparing(AnalyticsStockPosition::getBusinessDate))
                    .toList();
            final AnalyticsStockPosition latest = dimensionRows.get(dimensionRows.size() - 1);
            final int observedDayCount = dimensionRows.size();
            final int stockoutDayCount = (int) dimensionRows.stream()
                    .filter(row -> defaultInt(row.getAvailableQuantity()) <= 0)
                    .count();
            totalObservedDayCount += observedDayCount;
            totalStockoutDayCount += stockoutDayCount;
            final Metadata metadata = metadataBundle.metadataFor(entry.getKey());
            rows.add(new StockoutRateReportResponse.StockoutRateRow(
                    entry.getKey().productId(),
                    metadata.productName(),
                    entry.getKey().centerId(),
                    metadata.centerName(),
                    entry.getKey().warehouseId(),
                    metadata.warehouseName(),
                    observedDayCount,
                    stockoutDayCount,
                    ratio(stockoutDayCount, observedDayCount),
                    defaultInt(latest.getAvailableQuantity())));
        }

        final List<StockoutRateReportResponse.StockoutRateRow> sortedRows = rows.stream()
                .sorted(stockoutRowComparator())
                .toList();
        return new StockoutRateReportResponse(
                new StockoutRateReportResponse.StockoutRateSummary(
                        sortedRows.size(),
                        totalObservedDayCount,
                        totalStockoutDayCount,
                        ratio(totalStockoutDayCount, totalObservedDayCount)),
                sortedRows);
    }

    /**
     * Returns expiry-waste analytics.
     *
     * @param filter scoped analytics filter
     * @return expiry-waste response DTO
     */
    public ExpiryWasteReportResponse getExpiryWasteReport(final AnalyticsQueryFilter filter) {
        final AnalyticsQueryFilter validatedFilter = validateFilter(filter);
        final List<AnalyticsExpiryWaste> wasteRows = scopedAndDatedRows(
                loadExpiryWaste(validatedFilter),
                AnalyticsExpiryWaste::getCenterId,
                AnalyticsExpiryWaste::getWarehouseId,
                AnalyticsExpiryWaste::getBusinessDate,
                validatedFilter);
        final Map<DimensionKey, TotalsAccumulator> grouped = new HashMap<>();
        for (final AnalyticsExpiryWaste wasteRow : wasteRows) {
            grouped.computeIfAbsent(
                            new DimensionKey(wasteRow.getProductId(), wasteRow.getCenterId(), wasteRow.getWarehouseId()),
                            ignored -> new TotalsAccumulator())
                    .add(defaultInt(wasteRow.getQuarantinedQuantity()), defaultInt(wasteRow.getQuarantinedLotCount()));
        }
        final MetadataBundle metadataBundle = loadMetadata(grouped.keySet());
        final List<ExpiryWasteReportResponse.ExpiryWasteRow> rows = grouped.entrySet().stream()
                .map(entry -> {
                    final Metadata metadata = metadataBundle.metadataFor(entry.getKey());
                    return new ExpiryWasteReportResponse.ExpiryWasteRow(
                            entry.getKey().productId(),
                            metadata.productName(),
                            entry.getKey().centerId(),
                            metadata.centerName(),
                            entry.getKey().warehouseId(),
                            metadata.warehouseName(),
                            entry.getValue().primaryTotal,
                            entry.getValue().secondaryTotal);
                })
                .sorted(expiryWasteRowComparator())
                .toList();

        final int totalQuarantinedQuantity = rows.stream().mapToInt(ExpiryWasteReportResponse.ExpiryWasteRow::quarantinedQuantity).sum();
        final int totalQuarantinedLotCount = rows.stream().mapToInt(ExpiryWasteReportResponse.ExpiryWasteRow::quarantinedLotCount).sum();
        return new ExpiryWasteReportResponse(
                new ExpiryWasteReportResponse.ExpiryWasteSummary(rows.size(), totalQuarantinedQuantity, totalQuarantinedLotCount),
                rows);
    }

    /**
     * Returns purchase-order lead-time analytics.
     *
     * @param filter scoped analytics filter
     * @return lead-time response DTO
     */
    public PurchaseOrderLeadTimeReportResponse getPurchaseOrderLeadTimeReport(final AnalyticsQueryFilter filter) {
        final AnalyticsQueryFilter validatedFilter = validateFilter(filter);
        final List<AnalyticsPurchaseOrderLeadTime> leadTimeRows = scopedAndDatedRows(
                loadLeadTimeRows(validatedFilter),
                AnalyticsPurchaseOrderLeadTime::getCenterId,
                AnalyticsPurchaseOrderLeadTime::getWarehouseId,
                AnalyticsPurchaseOrderLeadTime::getBusinessDate,
                validatedFilter);
        final Map<DimensionKey, LeadTimeAccumulator> grouped = new HashMap<>();
        for (final AnalyticsPurchaseOrderLeadTime leadTimeRow : leadTimeRows) {
            grouped.computeIfAbsent(
                            new DimensionKey(leadTimeRow.getProductId(), leadTimeRow.getCenterId(), leadTimeRow.getWarehouseId()),
                            ignored -> new LeadTimeAccumulator())
                    .add(
                            defaultInt(leadTimeRow.getPurchaseOrderCount()),
                            defaultInt(leadTimeRow.getLeadTimeSampleCount()),
                            defaultLong(leadTimeRow.getTotalLeadTimeHours()));
        }
        final MetadataBundle metadataBundle = loadMetadata(grouped.keySet());
        final List<PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow> rows = grouped.entrySet().stream()
                .map(entry -> {
                    final Metadata metadata = metadataBundle.metadataFor(entry.getKey());
                    return new PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow(
                            entry.getKey().productId(),
                            metadata.productName(),
                            entry.getKey().centerId(),
                            metadata.centerName(),
                            entry.getKey().warehouseId(),
                            metadata.warehouseName(),
                            entry.getValue().purchaseOrderCount,
                            entry.getValue().sampleCount,
                            entry.getValue().totalLeadTimeHours,
                            decimalAverage(entry.getValue().totalLeadTimeHours, entry.getValue().sampleCount));
                })
                .sorted(leadTimeRowComparator())
                .toList();

        final int totalPurchaseOrders = rows.stream().mapToInt(PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow::purchaseOrderCount).sum();
        final int totalSamples = rows.stream().mapToInt(PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow::leadTimeSampleCount).sum();
        final long totalLeadTimeHours = rows.stream().mapToLong(PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow::totalLeadTimeHours).sum();
        return new PurchaseOrderLeadTimeReportResponse(
                new PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeSummary(
                        rows.size(),
                        totalPurchaseOrders,
                        totalSamples,
                        totalLeadTimeHours,
                        decimalAverage(totalLeadTimeHours, totalSamples)),
                rows);
    }

    /**
     * Returns fill-rate analytics.
     *
     * @param filter scoped analytics filter
     * @return fill-rate response DTO
     */
    public FillRateReportResponse getFillRateReport(final AnalyticsQueryFilter filter) {
        final AnalyticsQueryFilter validatedFilter = validateFilter(filter);
        final List<AnalyticsFillRateSource> fillRateRows = scopedAndDatedRows(
                loadFillRateRows(validatedFilter),
                AnalyticsFillRateSource::getCenterId,
                AnalyticsFillRateSource::getWarehouseId,
                AnalyticsFillRateSource::getBusinessDate,
                validatedFilter);
        final Map<DimensionKey, FillRateAccumulator> grouped = new HashMap<>();
        for (final AnalyticsFillRateSource fillRateRow : fillRateRows) {
            grouped.computeIfAbsent(
                            new DimensionKey(fillRateRow.getProductId(), fillRateRow.getCenterId(), fillRateRow.getWarehouseId()),
                            ignored -> new FillRateAccumulator())
                    .add(
                            defaultInt(fillRateRow.getPurchaseOrderCount()),
                            defaultInt(fillRateRow.getRequestedQuantity()),
                            defaultInt(fillRateRow.getAcceptedQuantity()),
                            defaultInt(fillRateRow.getCancelledQuantity()),
                            defaultInt(fillRateRow.getShippedQuantity()));
        }
        final MetadataBundle metadataBundle = loadMetadata(grouped.keySet());
        final List<FillRateReportResponse.FillRateRow> rows = grouped.entrySet().stream()
                .map(entry -> {
                    final Metadata metadata = metadataBundle.metadataFor(entry.getKey());
                    return new FillRateReportResponse.FillRateRow(
                            entry.getKey().productId(),
                            metadata.productName(),
                            entry.getKey().centerId(),
                            metadata.centerName(),
                            entry.getKey().warehouseId(),
                            metadata.warehouseName(),
                            entry.getValue().purchaseOrderCount,
                            entry.getValue().requestedQuantity,
                            entry.getValue().acceptedQuantity,
                            entry.getValue().cancelledQuantity,
                            entry.getValue().shippedQuantity,
                            percentage(entry.getValue().acceptedQuantity, entry.getValue().requestedQuantity),
                            percentage(entry.getValue().shippedQuantity, entry.getValue().requestedQuantity));
                })
                .sorted(fillRateRowComparator())
                .toList();

        final int totalPurchaseOrders = rows.stream().mapToInt(FillRateReportResponse.FillRateRow::purchaseOrderCount).sum();
        final int totalRequestedQuantity = rows.stream().mapToInt(FillRateReportResponse.FillRateRow::requestedQuantity).sum();
        final int totalAcceptedQuantity = rows.stream().mapToInt(FillRateReportResponse.FillRateRow::acceptedQuantity).sum();
        final int totalCancelledQuantity = rows.stream().mapToInt(FillRateReportResponse.FillRateRow::cancelledQuantity).sum();
        final int totalShippedQuantity = rows.stream().mapToInt(FillRateReportResponse.FillRateRow::shippedQuantity).sum();
        return new FillRateReportResponse(
                new FillRateReportResponse.FillRateSummary(
                        rows.size(),
                        totalPurchaseOrders,
                        totalRequestedQuantity,
                        totalAcceptedQuantity,
                        totalCancelledQuantity,
                        totalShippedQuantity,
                        percentage(totalAcceptedQuantity, totalRequestedQuantity),
                        percentage(totalShippedQuantity, totalRequestedQuantity)),
                rows);
    }

    private StockAgingReportResponse.StockAgingRow toStockAgingRow(final DimensionKey key,
                                                                   final AnalyticsStockPosition latestPosition,
                                                                   final int confirmedDemandQuantity,
                                                                   final int observedDayCount,
                                                                   final MetadataBundle metadataBundle) {
        final Metadata metadata = metadataBundle.metadataFor(key);
        final int availableQuantity = defaultInt(latestPosition.getAvailableQuantity());
        final BigDecimal averageDailyDemand = decimalAverage(confirmedDemandQuantity, observedDayCount);
        final BigDecimal estimatedCoverageDays = averageDailyDemand.signum() == 0
                ? null
                : BigDecimal.valueOf(availableQuantity).divide(averageDailyDemand, 2, RoundingMode.HALF_UP);
        return new StockAgingReportResponse.StockAgingRow(
                key.productId(),
                metadata.productName(),
                key.centerId(),
                metadata.centerName(),
                key.warehouseId(),
                metadata.warehouseName(),
                latestPosition.getBusinessDate(),
                availableQuantity,
                averageDailyDemand,
                estimatedCoverageDays,
                agingBucketLabel(estimatedCoverageDays));
    }

    private AnalyticsQueryFilter validateFilter(final AnalyticsQueryFilter filter) {
        if (filter == null) {
            throw new InvalidOperationException("Analytics filter is required");
        }
        if (filter.from() != null && filter.to() != null && filter.to().isBefore(filter.from())) {
            throw new InvalidOperationException("Analytics date range is invalid");
        }
        if (filter.warehouseId() != null) {
            scopeGuard.assertWarehouseAccess(filter.warehouseId());
        } else if (filter.centerId() != null) {
            scopeGuard.assertCenterAccess(filter.centerId());
        }
        return filter;
    }

    private List<AnalyticsDemandHistory> loadDemandHistory(final AnalyticsQueryFilter filter) {
        if (filter.from() != null && filter.to() != null) {
            return analyticsDemandHistoryRepository.findByBusinessDateBetweenOrderByBusinessDateAsc(filter.from(), filter.to());
        }
        return analyticsDemandHistoryRepository.findAll();
    }

    private List<AnalyticsStockPosition> loadStockPositions(final AnalyticsQueryFilter filter) {
        if (filter.from() != null && filter.to() != null) {
            return analyticsStockPositionRepository.findByBusinessDateBetweenOrderByBusinessDateAsc(filter.from(), filter.to());
        }
        return analyticsStockPositionRepository.findAll();
    }

    private List<AnalyticsExpiryWaste> loadExpiryWaste(final AnalyticsQueryFilter filter) {
        if (filter.from() != null && filter.to() != null) {
            return analyticsExpiryWasteRepository.findByBusinessDateBetweenOrderByBusinessDateAsc(filter.from(), filter.to());
        }
        return analyticsExpiryWasteRepository.findAll();
    }

    private List<AnalyticsPurchaseOrderLeadTime> loadLeadTimeRows(final AnalyticsQueryFilter filter) {
        if (filter.from() != null && filter.to() != null) {
            return analyticsPurchaseOrderLeadTimeRepository.findByBusinessDateBetweenOrderByBusinessDateAsc(filter.from(), filter.to());
        }
        return analyticsPurchaseOrderLeadTimeRepository.findAll();
    }

    private List<AnalyticsFillRateSource> loadFillRateRows(final AnalyticsQueryFilter filter) {
        if (filter.from() != null && filter.to() != null) {
            return analyticsFillRateSourceRepository.findByBusinessDateBetweenOrderByBusinessDateAsc(filter.from(), filter.to());
        }
        return analyticsFillRateSourceRepository.findAll();
    }

    private <T> List<T> scopedAndDatedRows(final Collection<T> rows,
                                           final Function<T, Long> centerIdExtractor,
                                           final Function<T, Long> warehouseIdExtractor,
                                           final Function<T, LocalDate> businessDateExtractor,
                                           final AnalyticsQueryFilter filter) {
        return scopeGuard.filterByCenterWarehouseScope(rows, centerIdExtractor, warehouseIdExtractor).stream()
                .filter(row -> matchesExplicitScope(row, centerIdExtractor, warehouseIdExtractor, filter))
                .filter(row -> matchesDateRange(businessDateExtractor.apply(row), filter.from(), filter.to()))
                .toList();
    }

    private <T> boolean matchesExplicitScope(final T row,
                                             final Function<T, Long> centerIdExtractor,
                                             final Function<T, Long> warehouseIdExtractor,
                                             final AnalyticsQueryFilter filter) {
        final Long centerId = centerIdExtractor.apply(row);
        final Long warehouseId = warehouseIdExtractor.apply(row);
        final boolean matchesCenter = filter.centerId() == null || Objects.equals(filter.centerId(), centerId);
        final boolean matchesWarehouse = filter.warehouseId() == null || Objects.equals(filter.warehouseId(), warehouseId);
        return matchesCenter && matchesWarehouse;
    }

    private boolean matchesDateRange(final LocalDate businessDate, final LocalDate from, final LocalDate to) {
        if (businessDate == null) {
            return false;
        }
        final boolean matchesFrom = from == null || !businessDate.isBefore(from);
        final boolean matchesTo = to == null || !businessDate.isAfter(to);
        return matchesFrom && matchesTo;
    }

    private int resolveObservedDayCount(final AnalyticsQueryFilter filter, final List<LocalDate> businessDates) {
        if (filter.from() != null && filter.to() != null) {
            return (int) ChronoUnit.DAYS.between(filter.from(), filter.to()) + 1;
        }
        if (businessDates.isEmpty()) {
            return 0;
        }
        if (filter.from() != null) {
            final LocalDate latest = businessDates.stream().max(LocalDate::compareTo).orElse(filter.from());
            return latest.isBefore(filter.from()) ? 0 : (int) ChronoUnit.DAYS.between(filter.from(), latest) + 1;
        }
        if (filter.to() != null) {
            final LocalDate earliest = businessDates.stream().min(LocalDate::compareTo).orElse(filter.to());
            return earliest.isAfter(filter.to()) ? 0 : (int) ChronoUnit.DAYS.between(earliest, filter.to()) + 1;
        }
        return (int) businessDates.stream().distinct().count();
    }

    private MetadataBundle loadMetadata(final Set<DimensionKey> keys) {
        final Set<Long> productIds = keys.stream().map(DimensionKey::productId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        final Set<Long> centerIds = keys.stream().map(DimensionKey::centerId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        final Set<Long> warehouseIds = keys.stream().map(DimensionKey::warehouseId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());

        final Map<Long, String> productNames = productRepository.findAllById(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, Product::getName));
        final Map<Long, String> centerNames = centerRepository.findAllById(centerIds).stream()
                .collect(java.util.stream.Collectors.toMap(Center::getId, Center::getName));
        final Map<Long, String> warehouseNames = warehouseRepository.findAllById(warehouseIds).stream()
                .collect(java.util.stream.Collectors.toMap(Warehouse::getId, Warehouse::getName));
        return new MetadataBundle(productNames, centerNames, warehouseNames);
    }

    private <T> Map<DimensionKey, List<T>> groupByDimension(final Collection<T> rows,
                                                            final Function<T, DimensionKey> keyExtractor) {
        final Map<DimensionKey, List<T>> grouped = new HashMap<>();
        for (final T row : rows) {
            grouped.computeIfAbsent(keyExtractor.apply(row), ignored -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private BigDecimal decimalAverage(final long total, final int divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(final int numerator, final int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(final int numerator, final int denominator) {
        return ratio(numerator, denominator).multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    private int defaultInt(final Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(final Long value) {
        return value == null ? 0L : value;
    }

    private String agingBucketLabel(final BigDecimal estimatedCoverageDays) {
        if (estimatedCoverageDays == null) {
            return "NO_DEMAND";
        }
        if (estimatedCoverageDays.compareTo(BigDecimal.valueOf(30)) <= 0) {
            return "0-30_DAYS";
        }
        if (estimatedCoverageDays.compareTo(BigDecimal.valueOf(60)) <= 0) {
            return "31-60_DAYS";
        }
        if (estimatedCoverageDays.compareTo(BigDecimal.valueOf(90)) <= 0) {
            return "61-90_DAYS";
        }
        return "90+_DAYS";
    }

    private Comparator<StockAgingReportResponse.StockAgingRow> stockRowComparator() {
        return Comparator.comparing(StockAgingReportResponse.StockAgingRow::centerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(StockAgingReportResponse.StockAgingRow::warehouseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(StockAgingReportResponse.StockAgingRow::productName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<StockoutRateReportResponse.StockoutRateRow> stockoutRowComparator() {
        return Comparator.comparing(StockoutRateReportResponse.StockoutRateRow::centerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(StockoutRateReportResponse.StockoutRateRow::warehouseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(StockoutRateReportResponse.StockoutRateRow::productName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<ExpiryWasteReportResponse.ExpiryWasteRow> expiryWasteRowComparator() {
        return Comparator.comparing(ExpiryWasteReportResponse.ExpiryWasteRow::centerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ExpiryWasteReportResponse.ExpiryWasteRow::warehouseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ExpiryWasteReportResponse.ExpiryWasteRow::productName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow> leadTimeRowComparator() {
        return Comparator.comparing(PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow::centerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow::warehouseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PurchaseOrderLeadTimeReportResponse.PurchaseOrderLeadTimeRow::productName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<FillRateReportResponse.FillRateRow> fillRateRowComparator() {
        return Comparator.comparing(FillRateReportResponse.FillRateRow::centerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(FillRateReportResponse.FillRateRow::warehouseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(FillRateReportResponse.FillRateRow::productName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private record DimensionKey(Long productId, Long centerId, Long warehouseId) {
    }

    private record Metadata(String productName, String centerName, String warehouseName) {
    }

    private record MetadataBundle(
            Map<Long, String> productNames,
            Map<Long, String> centerNames,
            Map<Long, String> warehouseNames
    ) {
        private Metadata metadataFor(final DimensionKey key) {
            return new Metadata(
                    productNames.getOrDefault(key.productId(), "Unknown Product"),
                    centerNames.getOrDefault(key.centerId(), "Unknown Center"),
                    warehouseNames.getOrDefault(key.warehouseId(), "Unknown Warehouse"));
        }
    }

    private static final class TotalsAccumulator {

        private int primaryTotal;
        private int secondaryTotal;

        private void add(final int primaryDelta, final int secondaryDelta) {
            this.primaryTotal += primaryDelta;
            this.secondaryTotal += secondaryDelta;
        }
    }

    private static final class LeadTimeAccumulator {

        private int purchaseOrderCount;
        private int sampleCount;
        private long totalLeadTimeHours;

        private void add(final int purchaseOrderDelta, final int sampleDelta, final long leadTimeHoursDelta) {
            this.purchaseOrderCount += purchaseOrderDelta;
            this.sampleCount += sampleDelta;
            this.totalLeadTimeHours += leadTimeHoursDelta;
        }
    }

    private static final class FillRateAccumulator {

        private int purchaseOrderCount;
        private int requestedQuantity;
        private int acceptedQuantity;
        private int cancelledQuantity;
        private int shippedQuantity;

        private void add(final int purchaseOrderDelta,
                         final int requestedDelta,
                         final int acceptedDelta,
                         final int cancelledDelta,
                         final int shippedDelta) {
            this.purchaseOrderCount += purchaseOrderDelta;
            this.requestedQuantity += requestedDelta;
            this.acceptedQuantity += acceptedDelta;
            this.cancelledQuantity += cancelledDelta;
            this.shippedQuantity += shippedDelta;
        }
    }

    public AnalyticsReportingService(final AnalyticsDemandHistoryRepository analyticsDemandHistoryRepository, final AnalyticsStockPositionRepository analyticsStockPositionRepository, final AnalyticsExpiryWasteRepository analyticsExpiryWasteRepository, final AnalyticsPurchaseOrderLeadTimeRepository analyticsPurchaseOrderLeadTimeRepository, final AnalyticsFillRateSourceRepository analyticsFillRateSourceRepository, final ProductRepository productRepository, final CenterRepository centerRepository, final WarehouseRepository warehouseRepository, final ScopeGuard scopeGuard) {
        this.analyticsDemandHistoryRepository = analyticsDemandHistoryRepository;
        this.analyticsStockPositionRepository = analyticsStockPositionRepository;
        this.analyticsExpiryWasteRepository = analyticsExpiryWasteRepository;
        this.analyticsPurchaseOrderLeadTimeRepository = analyticsPurchaseOrderLeadTimeRepository;
        this.analyticsFillRateSourceRepository = analyticsFillRateSourceRepository;
        this.productRepository = productRepository;
        this.centerRepository = centerRepository;
        this.warehouseRepository = warehouseRepository;
        this.scopeGuard = scopeGuard;
    }
}
