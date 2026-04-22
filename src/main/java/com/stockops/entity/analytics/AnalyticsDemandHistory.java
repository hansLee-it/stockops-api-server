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
 * Daily confirmed-demand analytics row for a product and warehouse scope.
 * This table is the canonical demand source for BI and AI because it only stores confirmed outbound demand.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Entity
@Table(
        schema = "analytics",
        name = "daily_demand_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analytics_daily_demand",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyticsDemandHistory extends BaseEntity {

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

    @Column(name = "confirmed_outbound_quantity", nullable = false)
    private Integer confirmedOutboundQuantity;

    @Column(name = "confirmed_outbound_event_count", nullable = false)
    private Integer confirmedOutboundEventCount;

    @Column(name = "insufficient_history", nullable = false)
    private boolean insufficientHistory;
}
