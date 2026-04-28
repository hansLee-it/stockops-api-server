package com.stockops.repository;

import com.stockops.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository for Product entity persistence and queries.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedFalse(Long id);

    Optional<Product> findByBarcodeAndDeletedFalse(String barcode);

    boolean existsByBarcodeAndDeletedFalse(String barcode);

    Page<Product> findAllByDeletedFalse(Pageable pageable);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    /**
     * Finds all active (non-deleted) products belonging to a specific category.
     *
     * @param categoryId category identifier
     * @return list of products in the category
     */
    List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);

    /**
     * Finds all active products belonging to any of the given categories.
     *
     * @param categoryIds collection of category identifiers
     * @return list of products in the specified categories
     */
    List<Product> findByCategoryIdInAndDeletedFalse(List<Long> categoryIds);
}
