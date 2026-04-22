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
 * Daily purchase-order lead-time source row.
 * Lead time is tracked as requested-to-ERP-response hours so downstream analytics can compute averages deterministically.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Entity
@Table(
        schema = "analytics",
        name = "daily_purchase_order_lead_time",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_po_lead_time",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyticsPurchaseOrderLeadTime extends BaseEntity {

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

    @Column(name = "lead_time_sample_count", nullable = false)
    private Integer leadTimeSampleCount;

    @Column(name = "total_lead_time_hours", nullable = false)
    private Long totalLeadTimeHours;
}
