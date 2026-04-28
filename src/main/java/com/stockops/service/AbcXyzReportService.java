package com.stockops.service;

import com.stockops.dto.AbcClassificationDTO;
import com.stockops.dto.AbcXyzMatrixDTO;
import com.stockops.dto.XyzClassificationDTO;
import com.stockops.entity.Center;
import com.stockops.entity.InventoryTransaction;
import com.stockops.entity.Location;
import com.stockops.entity.Product;
import com.stockops.entity.Warehouse;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.InventoryTransactionRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.repository.WarehouseRepository;
import com.stockops.security.ScopeGuard;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ABC/XYZ inventory classification service.
 *
 * <p>ABC analysis ranks products by annual usage value (quantity × unit price):
 * <ul>
 *   <li>A: top 80% of cumulative value</li>
 *   <li>B: next 15% (80–95%)</li>
 *   <li>C: bottom 5% (95–100%)</li>
 * </ul>
 *
 * <p>XYZ analysis measures demand variability using coefficient of variation:
 * <ul>
 *   <li>X: CV &lt; 50% (stable demand)</li>
 *   <li>Y: CV 50–100% (variable demand)</li>
 *   <li>Z: CV &gt; 100% (erratic demand)</li>
 * </ul>
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbcXyzReportService {

    private static final String TYPE_OUTBOUND = "OUTBOUND";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ABC_A_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal ABC_B_THRESHOLD = BigDecimal.valueOf(95);
    private static final BigDecimal XYZ_Y_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal XYZ_Z_THRESHOLD = BigDecimal.valueOf(100);

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final CenterRepository centerRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ScopeGuard scopeGuard;

    /**
     * Performs ABC analysis for products in a center.
     *
     * @param centerId center identifier (required)
     * @return ordered list of ABC classification results sorted by usage value descending
     * @throws ResourceNotFoundException when center does not exist
     */
    public List<AbcClassificationDTO> getAbcAnalysis(final Long centerId) {
        scopeGuard.assertCenterAccess(centerId);
        final Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found: " + centerId));

        final List<Long> locationIds = resolveLocationIds(centerId);
        if (locationIds.isEmpty()) {
            return List.of();
        }

        final Instant oneYearAgo = Instant.now().minusSeconds(365L * 24 * 60 * 60);
        final List<InventoryTransaction> outboundTransactions =
                transactionRepository.findOutboundByLocationIdsAndCreatedAtBetween(locationIds, oneYearAgo, Instant.now());

        final Map<Long, BigDecimal> productDefaultPrices = loadProductDefaultPrices();
        final Map<Long, Integer> productTotalQuantities = new HashMap<>();
        for (final InventoryTransaction tx : outboundTransactions) {
            productTotalQuantities.merge(tx.getProductId(), tx.getQuantity(), Integer::sum);
        }

        final List<ProductUsage> productUsages = new ArrayList<>();
        for (final Map.Entry<Long, Integer> entry : productTotalQuantities.entrySet()) {
            final BigDecimal unitPrice = productDefaultPrices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            final BigDecimal usageValue = unitPrice.multiply(BigDecimal.valueOf(entry.getValue()));
            productUsages.add(new ProductUsage(entry.getKey(), entry.getValue(), usageValue));
        }

        productUsages.sort(Comparator.comparing(ProductUsage::usageValue).reversed());

        final BigDecimal totalValue = productUsages.stream()
                .map(ProductUsage::usageValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final Map<Long, String> productNames = loadProductNames();
        final List<AbcClassificationDTO> results = new ArrayList<>();
        BigDecimal cumulativeValue = BigDecimal.ZERO;

        for (final ProductUsage pu : productUsages) {
            cumulativeValue = cumulativeValue.add(pu.usageValue());
            final BigDecimal cumulativePercentage = totalValue.signum() == 0
                    ? BigDecimal.ZERO
                    : cumulativeValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
            final String abcClass = classifyAbc(cumulativePercentage);
            results.add(new AbcClassificationDTO(
                    pu.productId(),
                    productNames.getOrDefault(pu.productId(), "Unknown Product"),
                    pu.usageValue(),
                    cumulativePercentage,
                    abcClass));
        }

        return results;
    }

    /**
     * Performs XYZ analysis for products in a center.
     *
     * @param centerId center identifier (required)
     * @return list of XYZ classification results
     * @throws ResourceNotFoundException when center does not exist
     */
    public List<XyzClassificationDTO> getXyzAnalysis(final Long centerId) {
        scopeGuard.assertCenterAccess(centerId);
        final Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found: " + centerId));

        final List<Long> locationIds = resolveLocationIds(centerId);
        if (locationIds.isEmpty()) {
            return List.of();
        }

        final Instant twelveMonthsAgo = Instant.now().minusSeconds(365L * 24 * 60 * 60);
        final List<InventoryTransaction> outboundTransactions =
                transactionRepository.findOutboundByLocationIdsAndCreatedAtBetween(locationIds, twelveMonthsAgo, Instant.now());

        final Map<Long, Map<YearMonth, Integer>> productMonthlyDemand = new HashMap<>();
        for (final InventoryTransaction tx : outboundTransactions) {
            final YearMonth month = YearMonth.from(tx.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")));
            productMonthlyDemand.computeIfAbsent(tx.getProductId(), k -> new LinkedHashMap<>())
                    .merge(month, tx.getQuantity(), Integer::sum);
        }

        final Map<Long, String> productNames = loadProductNames();
        final List<XyzClassificationDTO> results = new ArrayList<>();

        for (final Map.Entry<Long, Map<YearMonth, Integer>> entry : productMonthlyDemand.entrySet()) {
            final long productId = entry.getKey();
            final List<Integer> monthlyQuantities = new ArrayList<>(entry.getValue().values());

            final BigDecimal mean = computeMean(monthlyQuantities);
            final BigDecimal stdDev = computeStdDev(monthlyQuantities, mean);
            final BigDecimal cv = mean.signum() == 0
                    ? BigDecimal.ZERO
                    : stdDev.divide(mean, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);

            final String xyzClass = classifyXyz(cv);
            results.add(new XyzClassificationDTO(
                    productId,
                    productNames.getOrDefault(productId, "Unknown Product"),
                    mean.setScale(2, RoundingMode.HALF_UP),
                    stdDev.setScale(2, RoundingMode.HALF_UP),
                    cv.setScale(2, RoundingMode.HALF_UP),
                    xyzClass));
        }

        results.sort(Comparator.comparing(XyzClassificationDTO::cv));
        return results;
    }

    /**
     * Builds the combined ABC-XYZ classification matrix for a center.
     *
     * @param centerId center identifier (required)
     * @return 3×3 matrix with product counts and details per cell
     * @throws ResourceNotFoundException when center does not exist
     */
    public AbcXyzMatrixDTO getAbcXyzMatrix(final Long centerId) {
        final List<AbcClassificationDTO> abcResults = getAbcAnalysis(centerId);
        final List<XyzClassificationDTO> xyzResults = getXyzAnalysis(centerId);

        final Map<Long, AbcClassificationDTO> abcByProduct = abcResults.stream()
                .collect(Collectors.toMap(AbcClassificationDTO::productId, dto -> dto));
        final Map<Long, XyzClassificationDTO> xyzByProduct = xyzResults.stream()
                .collect(Collectors.toMap(XyzClassificationDTO::productId, dto -> dto));

        final Map<String, List<AbcXyzMatrixDTO.MatrixCellProduct>> matrixData = new HashMap<>();
        for (final AbcClassificationDTO abc : abcResults) {
            final XyzClassificationDTO xyz = xyzByProduct.get(abc.productId());
            if (xyz == null) {
                continue;
            }
            final String key = abc.abcClass() + "_" + classifyXyz(xyz.cv());
            matrixData.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new AbcXyzMatrixDTO.MatrixCellProduct(
                            abc.productId(),
                            abc.productName(),
                            abc.annualUsageValue(),
                            xyz.cv()));
        }

        final List<AbcXyzMatrixDTO.MatrixRow> rows = new ArrayList<>();
        for (final String abcClass : List.of("A", "B", "C")) {
            final List<AbcXyzMatrixDTO.MatrixCellProduct> xProducts =
                    matrixData.getOrDefault(abcClass + "_X", List.of());
            final List<AbcXyzMatrixDTO.MatrixCellProduct> yProducts =
                    matrixData.getOrDefault(abcClass + "_Y", List.of());
            final List<AbcXyzMatrixDTO.MatrixCellProduct> zProducts =
                    matrixData.getOrDefault(abcClass + "_Z", List.of());
            rows.add(new AbcXyzMatrixDTO.MatrixRow(
                    abcClass,
                    xProducts.size(),
                    yProducts.size(),
                    zProducts.size(),
                    xProducts,
                    yProducts,
                    zProducts));
        }

        final int totalProducts = abcResults.size();
        return new AbcXyzMatrixDTO(rows, totalProducts);
    }

    private List<Long> resolveLocationIds(final Long centerId) {
        final List<Warehouse> warehouses = warehouseRepository.findByCenterId(centerId);
        if (warehouses.isEmpty()) {
            return List.of();
        }
        final List<Long> warehouseIds = warehouses.stream().map(Warehouse::getId).toList();
        return locationRepository.findByWarehouseIdIn(warehouseIds).stream()
                .map(Location::getId)
                .toList();
    }

    private Map<Long, BigDecimal> loadProductDefaultPrices() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getDefaultPrice));
    }

    private Map<Long, String> loadProductNames() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    private String classifyAbc(final BigDecimal cumulativePercentage) {
        if (cumulativePercentage.compareTo(ABC_A_THRESHOLD) <= 0) {
            return "A";
        }
        if (cumulativePercentage.compareTo(ABC_B_THRESHOLD) <= 0) {
            return "B";
        }
        return "C";
    }

    private String classifyXyz(final BigDecimal cv) {
        if (cv.compareTo(XYZ_Y_THRESHOLD) < 0) {
            return "X";
        }
        if (cv.compareTo(XYZ_Z_THRESHOLD) <= 0) {
            return "Y";
        }
        return "Z";
    }

    private BigDecimal computeMean(final List<Integer> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        final BigDecimal sum = BigDecimal.valueOf(values.stream().mapToLong(Integer::longValue).sum());
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeStdDev(final List<Integer> values, final BigDecimal mean) {
        if (values.size() <= 1) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumSquares = BigDecimal.ZERO;
        for (final int value : values) {
            final BigDecimal diff = BigDecimal.valueOf(value).subtract(mean);
            sumSquares = sumSquares.add(diff.multiply(diff));
        }
        final BigDecimal variance = sumSquares.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(4, RoundingMode.HALF_UP);
    }

    private record ProductUsage(Long productId, int totalQuantity, BigDecimal usageValue) {
    }
}