package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Expiry alert snapshot for a lot that is approaching its expiration date.
 * Records the lot quantity and alert severity calculated during the daily alert refresh.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "expiry_alerts")
@NoArgsConstructor
@EntityListeners(com.stockops.audit.MutationAuditEntityListener.class)
public class ExpiryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "days_until_expiry", nullable = false)
    private Integer daysUntilExpiry;

    @Column(name = "alert_level", nullable = false, length = 20)
    private String alertLevel;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "is_acknowledged", nullable = false)
    private boolean acknowledged = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Initializes the created timestamp when the alert row is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
