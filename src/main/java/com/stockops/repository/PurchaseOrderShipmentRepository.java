package com.stockops.repository;

import com.stockops.entity.PurchaseOrderShipment;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderShipmentRepository extends JpaRepository<PurchaseOrderShipment, Long> {

    List<PurchaseOrderShipment> findByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Returns all shipments whose ETA has passed but have not yet been delivered.
     * Used by the Bedrock Agent {@code getPurchaseOrderDelaySummary} tool.
     *
     * @param etaDate upper bound (exclusive) — typically {@code LocalDate.now()}
     * @return overdue shipments
     */
    List<PurchaseOrderShipment> findByEtaDateBeforeAndDeliveredAtIsNull(LocalDate etaDate);
}
