package com.stockops.repository;

import com.stockops.entity.Lot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotRepository extends JpaRepository<Lot, Long> {

    Optional<Lot> findByLotNumberAndProductId(String lotNumber, Long productId);

    @Query("""
            SELECT l
            FROM Lot l
            WHERE l.productId = :productId
              AND l.status = com.stockops.entity.LotStatus.ACTIVE
            ORDER BY CASE WHEN l.expiryDate IS NULL THEN 1 ELSE 0 END,
                     l.expiryDate ASC,
                     l.id ASC
            """)
    List<Lot> findByProductIdOrderByExpiryDateAsc(@Param("productId") Long productId);
}
