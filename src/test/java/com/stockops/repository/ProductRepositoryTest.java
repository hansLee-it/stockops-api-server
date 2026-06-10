package com.stockops.repository;

import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import com.stockops.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataJpaTest verifying {@link ProductRepository#countProductsBelowSafetyStock()}.
 *
 * <p>Validates the JPQL correlated-subquery approach directly against an H2 in-memory
 * database — Mockito stubs cannot catch JPQL syntax or semantic errors at Hibernate parse time.
 *
 * <p>Flyway is disabled via {@code @TestPropertySource} because the PostgreSQL-specific migration
 * SQL cannot run on the fresh H2 embedded database created by {@code @DataJpaTest}. Instead,
 * {@code ddl-auto=create-drop} lets Hibernate build the schema from entity annotations.
 *
 * <p>Key boundary cases:
 * <ul>
 *   <li>Product with no inventory rows at all → available=0 → counted (critical stockout)</li>
 *   <li>Product with inventory available &lt; safety stock → counted</li>
 *   <li>Product with inventory available ≥ safety stock → NOT counted</li>
 *   <li>Product with safetyStockQuantity=0 → NOT counted (excluded by WHERE clause)</li>
 *   <li>Deleted product → NOT counted ({@code @SQLRestriction("deleted = false")} applied)</li>
 *   <li>Reserved quantity reduces effective available stock</li>
 * </ul>
 *
 * @author StockOps Team
 * @since 5.0
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository productRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Product persistProduct(final String barcode, final int safetyStock, final boolean deleted) {
        final Product p = new Product();
        p.setBarcode(barcode);
        p.setName("테스트 상품 " + barcode);
        p.setUnit("EA");
        p.setExpiryManaged(false);
        p.setDefaultPrice(BigDecimal.TEN);
        p.setSafetyStockQuantity(safetyStock);
        p.setDeleted(deleted);
        return em.persist(p);
    }

    private void persistInventory(final Long productId, final int qty, final int reserved) {
        final Inventory inv = new Inventory();
        inv.setProductId(productId);
        inv.setLocationId(99L);   // arbitrary; no FK constraint from plain Long column
        inv.setQuantity(qty);
        inv.setReservedQuantity(reserved);
        inv.setStatus(InventoryStatus.ACTIVE);
        em.persist(inv);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    /**
     * TS-P5-REPO-001: A product with safetyStock=10 and no inventory rows whatsoever
     * has effective available quantity = 0 (COALESCE of empty subquery = 0).
     * This is the critical "zero stock" case — must be counted.
     */
    @Test
    void countProductsBelowSafetyStock_noInventoryRows_isCounted() {
        persistProduct("BAR-NO-INV", 10, false);
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isEqualTo(1L);
    }

    /**
     * TS-P5-REPO-002: Available inventory (5) is below safety stock (10) → counted.
     */
    @Test
    void countProductsBelowSafetyStock_availableBelowSafety_isCounted() {
        final Product p = persistProduct("BAR-LOW", 10, false);
        persistInventory(p.getId(), 5, 0);   // available = 5
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isEqualTo(1L);
    }

    /**
     * TS-P5-REPO-003: Available inventory (15) meets safety stock (10) → NOT counted.
     */
    @Test
    void countProductsBelowSafetyStock_availableMeetsSafety_isNotCounted() {
        final Product p = persistProduct("BAR-OK", 10, false);
        persistInventory(p.getId(), 15, 0);  // available = 15
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isZero();
    }

    /**
     * TS-P5-REPO-004: Product with safetyStockQuantity=0 is excluded by
     * {@code WHERE p.safetyStockQuantity > 0} — such products have no safety target.
     */
    @Test
    void countProductsBelowSafetyStock_safetyStockZero_isExcluded() {
        persistProduct("BAR-ZERO-SAFETY", 0, false);
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isZero();
    }

    /**
     * TS-P5-REPO-005: Soft-deleted product with safetyStock=10 and no inventory is
     * excluded by {@code @SQLRestriction("deleted = false")}.
     */
    @Test
    void countProductsBelowSafetyStock_deletedProduct_isExcluded() {
        persistProduct("BAR-DELETED", 10, true);   // deleted=true
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isZero();
    }

    /**
     * TS-P5-REPO-006: Reserved quantity reduces effective available stock.
     * qty=12, reserved=5 → available=7 < safetyStock=10 → counted.
     */
    @Test
    void countProductsBelowSafetyStock_reservedReducesAvailable_isCounted() {
        final Product p = persistProduct("BAR-RESERVED", 10, false);
        persistInventory(p.getId(), 12, 5);  // available = 12 - 5 = 7
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isEqualTo(1L);
    }

    /**
     * TS-P5-REPO-007: Multiple products, mixed states — only below-threshold active ones counted.
     * Setup: below(2) + ok(1) + deleted-below(1) + zero-safety(1) → should return 2.
     */
    @Test
    void countProductsBelowSafetyStock_mixedProducts_countsOnlyBelowThreshold() {
        // Product A: safetyStock=10, no inventory → below (available=0)
        persistProduct("BAR-A-BELOW", 10, false);
        // Product B: safetyStock=10, available=8 → below
        final Product b = persistProduct("BAR-B-BELOW", 10, false);
        persistInventory(b.getId(), 8, 0);
        // Product C: safetyStock=10, available=15 → ok
        final Product c = persistProduct("BAR-C-OK", 10, false);
        persistInventory(c.getId(), 15, 0);
        // Product D: deleted, safetyStock=10, no inventory → excluded
        persistProduct("BAR-D-DEL", 10, true);
        // Product E: safetyStock=0, no inventory → excluded (no safety target)
        persistProduct("BAR-E-ZERO", 0, false);
        em.flush();

        assertThat(productRepository.countProductsBelowSafetyStock()).isEqualTo(2L);
    }
}
