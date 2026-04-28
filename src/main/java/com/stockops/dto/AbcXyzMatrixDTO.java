package com.stockops.dto;

import java.util.List;

/**
 * DTO for the combined ABC-XYZ classification matrix.
 * Cross-references ABC (value) and XYZ (variability) classifications into a 3×3 grid.
 *
 * @param rows matrix rows indexed by ABC class (A, B, C)
 * @param totalProductCount total number of classified products
 * @author StockOps Team
 * @since 2.0
 */
public record AbcXyzMatrixDTO(
        List<MatrixRow> rows,
        int totalProductCount) {

    /**
     * Single row in the ABC-XYZ matrix representing one ABC class.
     *
     * @param abcClass A, B, or C
     * @param xCount products classified as this ABC class AND X (stable)
     * @param yCount products classified as this ABC class AND Y (variable)
     * @param zCount products classified as this ABC class AND Z (erratic)
     * @param xProducts product details in the AX/BX/CX cell
     * @param yProducts product details in the AY/BY/CY cell
     * @param zProducts product details in the AZ/BZ/CZ cell
     */
    public record MatrixRow(
            String abcClass,
            int xCount,
            int yCount,
            int zCount,
            List<MatrixCellProduct> xProducts,
            List<MatrixCellProduct> yProducts,
            List<MatrixCellProduct> zProducts) {
    }

    /**
     * Product summary within a matrix cell.
     *
     * @param productId product identifier
     * @param productName product display name
     * @param annualUsageValue annual usage value (from ABC analysis)
     * @param cv coefficient of variation percentage (from XYZ analysis)
     */
    public record MatrixCellProduct(
            Long productId,
            String productName,
            java.math.BigDecimal annualUsageValue,
            java.math.BigDecimal cv) {
    }
}