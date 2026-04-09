package com.stockops.repository;

import com.stockops.entity.Inventory;
import com.stockops.entity.InventoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Loads a single inventory row with a pessimistic write lock.
     *
     * @param productId product id
     * @param locationId location id
     * @param lotId lot id
     * @return locked inventory row when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.productId = :productId
              AND i.locationId = :locationId
              AND ((:lotId IS NULL AND i.lotId IS NULL) OR i.lotId = :lotId)
            """)
    Optional<Inventory> findForUpdate(@Param("productId") Long productId,
                                      @Param("locationId") Long locationId,
                                      @Param("lotId") Long lotId);

    /**
     * Finds inventory without acquiring a lock.
     *
     * @param productId product id
     * @param locationId location id
     * @param lotId lot id
     * @return matching inventory row when present
     */
    Optional<Inventory> findByProductIdAndLocationIdAndLotId(Long productId, Long locationId, Long lotId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByLocationId(Long locationId);

    List<Inventory> findByLotId(Long lotId);

    /**
     * Loads an inventory row by id with a pessimistic write lock.
     *
     * @param id inventory id
     * @return locked inventory row when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);

    /**
     * Finds in-stock inventory rows for a product and lot sorted by location id.
     *
     * @param productId product id
     * @param lotId lot id
     * @param quantity minimum quantity threshold
     * @return matching inventory rows
     */
    List<Inventory> findByProductIdAndLotIdAndStatusAndQuantityGreaterThanOrderByLocationIdAsc(Long productId,
                                                                                                Long lotId,
                                                                                                InventoryStatus status,
                                                                                                Integer quantity);
}
