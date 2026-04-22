package com.stockops.entity.analytics;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Daily expiry-waste source row derived from automatic quarantine events.
 * Quantities are stored per product and warehouse so reporting can aggregate waste without recomputing domain joins.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Entity
@Table(
        schema = "analytics",
        name = "daily_expiry_waste",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_expiry_waste",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyticsExpiryWaste extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quarantined_quantity", nullable = false)
    private Integer quarantinedQuantity;

    @Column(name = "quarantined_lot_count", nullable = false)
    private Integer quarantinedLotCount;
}
