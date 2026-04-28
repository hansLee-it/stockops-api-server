package com.stockops.dto;

import java.math.BigDecimal;

/**
 * DTO for XYZ classification result of a single product.
 * XYZ analysis measures demand variability using the coefficient of variation (CV).
 *
 * <ul>
 *   <li>X: CV &lt; 50% (stable demand)</li>
 *   <li>Y: CV 50–100% (variable demand)</li>
 *   <li>Z: CV &gt; 100% (erratic demand)</li>
 * </ul>
 *
 * @param productId product identifier
 * @param productName product display name
 * @param meanDemand average monthly outbound quantity
 * @param stdDev standard deviation of monthly outbound quantities
 * @param cv coefficient of variation as percentage
 * @param xyzClass X, Y, or Z classification
 * @author StockOps Team
 * @since 2.0
 */
public record XyzClassificationDTO(
        Long productId,
        String productName,
        BigDecimal meanDemand,
        BigDecimal stdDev,
        BigDecimal cv,
        String xyzClass) {
}