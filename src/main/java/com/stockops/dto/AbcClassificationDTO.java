package com.stockops.dto;

import java.math.BigDecimal;

/**
 * DTO for ABC classification result of a single product.
 * ABC analysis ranks products by annual usage value (quantity × unit price).
 *
 * <ul>
 *   <li>A: top 80% of cumulative value</li>
 *   <li>B: next 15% (80–95%)</li>
 *   <li>C: bottom 5% (95–100%)</li>
 * </ul>
 *
 * @param productId product identifier
 * @param productName product display name
 * @param annualUsageValue total outbound value over the analysis period
 * @param cumulativePercentage cumulative percentage of total value
 * @param abcClass A, B, or C classification
 * @author StockOps Team
 * @since 2.0
 */
public record AbcClassificationDTO(
        Long productId,
        String productName,
        BigDecimal annualUsageValue,
        BigDecimal cumulativePercentage,
        String abcClass) {
}