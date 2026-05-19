package com.stockops.notification.escalation;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

/**
 * Escalation policy scoped by center/warehouse and alert type.
 * Contains 1-3 escalation rules defining multi-level notification behavior.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "escalation_policies", uniqueConstraints = @UniqueConstraint(
        name = "uk_escalation_policies_scope",
        columnNames = {"center_id", "warehouse_id", "alert_type"}))
public class EscalationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EscalationRule> rules = new ArrayList<>();

    public EscalationPolicy() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getCenterId() {
        return this.centerId;
    }

    public void setCenterId(final Long centerId) {
        this.centerId = centerId;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getAlertType() {
        return this.alertType;
    }

    public void setAlertType(final String alertType) {
        this.alertType = alertType;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public List<EscalationRule> getRules() {
        return this.rules;
    }

    public void setRules(final List<EscalationRule> rules) {
        this.rules = rules;
    }
}