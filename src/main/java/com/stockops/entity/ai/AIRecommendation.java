package com.stockops.entity.ai;

import com.stockops.entity.BaseEntity;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Persisted AI reorder recommendation snapshot for a product and warehouse scope.
 * Recommendations stay advisory until an authorized user approves them into a draft purchase order.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Data
@Entity
@Table(
        schema = "analytics",
        name = "ai_reorder_recommendations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_recommendation_scope_date",
                columnNames = {"business_date", "product_id", "center_id", "warehouse_id"}))
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AIRecommendation extends BaseEntity {

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_snapshot_id", nullable = false, unique = true)
    private AIForecastSnapshot forecastSnapshot;

    @Column(name = "current_stock_quantity", nullable = false)
    private Integer currentStockQuantity = 0;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Integer safetyStockQuantity = 0;

    @Column(name = "recommended_quantity", nullable = false)
    private Integer recommendedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AIRecommendationStatus status = AIRecommendationStatus.NO_ACTION;

    @Column(name = "explanation_summary", length = 500)
    private String explanationSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_purchase_order_id")
    private PurchaseOrder approvedPurchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
