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
 * Daily fill-rate source row for purchase-order reporting.
 * Requested, accepted, cancelled, and shipped quantities are stored without precomputed percentages so reporting stays reusable.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Entity
@Table(
        schema = "analytics",
        name = "daily_fill_rate_source",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_fill_rate",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyticsFillRateSource extends BaseEntity {

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

    @Column(name = "purchase_order_count", nullable = false)
    private Integer purchaseOrderCount;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;

    @Column(name = "cancelled_quantity", nullable = false)
    private Integer cancelledQuantity;

    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity;
}
